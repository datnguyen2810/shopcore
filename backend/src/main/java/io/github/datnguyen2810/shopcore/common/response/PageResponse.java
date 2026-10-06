package io.github.datnguyen2810.shopcore.common.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class PageResponse<T> {
    private final List<T> items;
    private final int currentPage;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;
//    first và last cho biết trang hiện tại có phải là trang đầu hoặc trang cuối hay không.
    private final boolean first;
    private final boolean last;


    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .items(page.getContent())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
