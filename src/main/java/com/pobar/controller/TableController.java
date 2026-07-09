package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.table.BarTableResponse;
import com.pobar.dto.table.BarTableVO;
import com.pobar.dto.table.OpenSessionRequest;
import com.pobar.dto.table.TableSessionPublicView;
import com.pobar.dto.table.TableSessionResponse;
import com.pobar.entity.BarTable;
import com.pobar.security.AuthUser;
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
    public Result<BarTableResponse> saveTable(@RequestBody BarTable table) {
        return Result.ok(BarTableResponse.from(tableService.saveTable(table)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<BarTableResponse> updateTable(@PathVariable Integer id, @RequestBody BarTable table) {
        table.setId(id);
        return Result.ok(BarTableResponse.from(tableService.saveTable(table)));
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
    public Result<TableSessionResponse> openSession(@Valid @RequestBody OpenSessionRequest request,
                                                     Authentication auth) {
        Integer userId = ((AuthUser) auth.getPrincipal()).id();
        return Result.ok(TableSessionResponse.from(tableService.openSession(request, userId)));
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

    /**
     * 客人掃 QR code 後呼叫，確認 session 有效（公開端點）。
     * 回傳極小 view，不暴露 openedById / openedAt 等內部欄位。
     */
    @GetMapping("/sessions/{token}")
    public Result<TableSessionPublicView> getSession(@PathVariable String token) {
        return Result.ok(TableSessionPublicView.from(tableService.getSessionByToken(token)));
    }
}
