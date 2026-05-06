package com.pobar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pobar.dto.table.BarTableVO;
import com.pobar.dto.table.OpenSessionRequest;
import com.pobar.entity.BarTable;
import com.pobar.entity.TableSession;
import com.pobar.exception.BusinessException;
import com.pobar.logging.Audit;
import com.pobar.mapper.BarTableMapper;
import com.pobar.mapper.TableSessionMapper;
import com.pobar.mapper.TableSessionTableMapper;
import com.pobar.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TableServiceImpl implements TableService {

    private final BarTableMapper barTableMapper;
    private final TableSessionMapper tableSessionMapper;
    private final TableSessionTableMapper tableSessionTableMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public List<BarTableVO> listTables() {
        return barTableMapper.listWithStatus();
    }

    @Override
    @Audit(action = "SAVE_TABLE", entityType = "TABLE")
    public BarTable saveTable(BarTable table) {
        if (table.getId() == null) {
            table.setIsActive(1);
            table.setIsLocked(0);
            barTableMapper.insert(table);
        } else {
            barTableMapper.updateById(table);
        }
        broadcastTableStatus();
        return table;
    }

    @Override
    @Audit(action = "DELETE_TABLE", entityType = "TABLE")
    public void deleteTable(Integer id) {
        if (tableSessionTableMapper.countActiveByTableId(id) > 0) {
            throw new BusinessException("此桌目前使用中，無法刪除");
        }
        barTableMapper.deleteById(id);
        broadcastTableStatus();
    }

    @Override
    @Transactional
    @Audit(action = "OPEN_SESSION", entityType = "TABLE_SESSION")
    public TableSession openSession(OpenSessionRequest request, Integer openedByUserId) {
        for (Integer tableId : request.getTableIds()) {
            if (tableSessionTableMapper.countActiveByTableId(tableId) > 0) {
                BarTable t = barTableMapper.selectById(tableId);
                throw new BusinessException("桌位 " + (t != null ? t.getName() : tableId) + " 目前使用中");
            }
        }

        TableSession session = new TableSession();
        session.setQrToken(UUID.randomUUID().toString());
        session.setStatus("OPEN");
        session.setPartySize(request.getPartySize());
        session.setOpenedAt(LocalDateTime.now());
        session.setOpenedById(openedByUserId);
        tableSessionMapper.insert(session);

        for (Integer tableId : request.getTableIds()) {
            tableSessionTableMapper.insert(session.getId(), tableId);
        }

        broadcastTableStatus();
        return session;
    }

    @Override
    @Transactional
    @Audit(action = "CLOSE_SESSION", entityType = "TABLE_SESSION")
    public void closeSession(Integer sessionId) {
        TableSession session = tableSessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException(404, "Session 不存在");
        if ("CLOSED".equals(session.getStatus())) throw new BusinessException("此桌已關閉");

        session.setStatus("CLOSED");
        session.setClosedAt(LocalDateTime.now());
        tableSessionMapper.updateById(session);

        broadcastTableStatus();
    }

    @Override
    @Transactional
    @Audit(action = "MERGE_TABLES", entityType = "TABLE_SESSION")
    public void mergeTables(Integer sessionId, List<Integer> additionalTableIds) {
        TableSession session = tableSessionMapper.selectById(sessionId);
        if (session == null || !"OPEN".equals(session.getStatus())) {
            throw new BusinessException("Session 不存在或已關閉");
        }
        for (Integer tableId : additionalTableIds) {
            if (tableSessionTableMapper.countActiveByTableId(tableId) > 0) {
                throw new BusinessException("桌位 " + tableId + " 目前使用中");
            }
            tableSessionTableMapper.insert(sessionId, tableId);
        }
        broadcastTableStatus();
    }

    @Override
    public TableSession getSessionByToken(String token) {
        TableSession session = tableSessionMapper.selectByToken(token);
        if (session == null) throw new BusinessException(404, "QR Code 無效");
        if (!"OPEN".equals(session.getStatus())) throw new BusinessException(410, "此桌已關閉，請聯絡服務人員");
        return session;
    }

    private void broadcastTableStatus() {
        List<BarTableVO> tables = listTables();
        messagingTemplate.convertAndSend("/topic/tables", tables);
    }
}
