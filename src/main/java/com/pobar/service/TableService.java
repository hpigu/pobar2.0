package com.pobar.service;

import com.pobar.dto.table.OpenSessionRequest;
import com.pobar.entity.BarTable;
import com.pobar.entity.TableSession;

import java.util.List;

public interface TableService {

    List<BarTable> listTables();
    BarTable saveTable(BarTable table);
    void deleteTable(Integer id);

    TableSession openSession(OpenSessionRequest request, Integer openedByUserId);
    void closeSession(Integer sessionId);
    void mergeTables(Integer sessionId, List<Integer> additionalTableIds);
    TableSession getSessionByToken(String token);
}
