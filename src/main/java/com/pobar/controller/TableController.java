package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.table.BarTableVO;
import com.pobar.dto.table.OpenSessionRequest;
import com.pobar.entity.BarTable;
import com.pobar.entity.TableSession;
import com.pobar.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<List<BarTableVO>> listTables() {
        return Result.ok(tableService.listTables());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<BarTable> saveTable(@RequestBody BarTable table) {
        return Result.ok(tableService.saveTable(table));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<BarTable> updateTable(@PathVariable Integer id, @RequestBody BarTable table) {
        table.setId(id);
        return Result.ok(tableService.saveTable(table));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<?> deleteTable(@PathVariable Integer id) {
        tableService.deleteTable(id);
        return Result.ok();
    }

    // ─── Session ─────────────────────────────────────────

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<TableSession> openSession(@Valid @RequestBody OpenSessionRequest request,
                                             Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return Result.ok(tableService.openSession(request, userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<?> closeSession(@PathVariable Integer sessionId) {
        tableService.closeSession(sessionId);
        return Result.ok();
    }

    @PostMapping("/sessions/{sessionId}/merge")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<?> mergeTables(@PathVariable Integer sessionId,
                                  @RequestBody List<Integer> additionalTableIds) {
        tableService.mergeTables(sessionId, additionalTableIds);
        return Result.ok();
    }

    // 客人掃 QR code 後呼叫，確認 session 有效（公開）
    @GetMapping("/sessions/{token}")
    public Result<TableSession> getSession(@PathVariable String token) {
        return Result.ok(tableService.getSessionByToken(token));
    }
}
