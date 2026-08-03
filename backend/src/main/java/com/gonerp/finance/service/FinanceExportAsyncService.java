package com.gonerp.finance.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Bean riêng để chạy build ZIP ngoài request thread. Phải tách khỏi
 * {@link FinanceExportService} vì @Async không có tác dụng khi gọi nội bộ trong
 * cùng một bean (không đi qua proxy) — và cũng nhờ vậy lời gọi markFailed() ở đây
 * mới mở được transaction REQUIRES_NEW.
 */
@Service
@RequiredArgsConstructor
public class FinanceExportAsyncService {

    private static final Logger log = LoggerFactory.getLogger(FinanceExportAsyncService.class);

    private final FinanceExportService exportService;

    @Async("financeExportExecutor")
    public void runGeneration(Long exportId) {
        try {
            exportService.buildAndStoreZip(exportId);
        } catch (Throwable t) {
            // Bắt Throwable chứ không chỉ Exception: dựng cả archive trong bộ nhớ có
            // thể ném OutOfMemoryError, khi đó bản ghi phải chuyển sang FAILED thay vì
            // kẹt mãi ở GENERATING và làm frontend poll vô hạn.
            log.error("Failed to generate report ZIP for export {}: {}", exportId, t.getMessage(), t);
            try {
                exportService.markFailed(exportId, t.getMessage());
            } catch (Exception e) {
                log.error("Could not mark export {} as FAILED: {}", exportId, e.getMessage(), e);
            }
        }
    }
}
