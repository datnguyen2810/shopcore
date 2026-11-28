package io.github.datnguyen2810.shopcore.category;

import io.github.datnguyen2810.shopcore.auth.security.JwtAccessDeniedHandler;
import io.github.datnguyen2810.shopcore.auth.security.JwtAuthenticationEntryPoint;
import io.github.datnguyen2810.shopcore.auth.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
        CategoryServiceImpl.class, CategoryMapper.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getCategories_withoutParameters_shouldReturnActivePageWithDefaults() throws Exception {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name", "id"));
        Category category = new Category("Phones", "phones", null);
        when(categoryRepository.searchCategories("", true, pageable))
                .thenReturn(new PageImpl<>(List.of(category), pageable, 1));

        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Phones"))
                .andExpect(jsonPath("$.data.items[0].active").value(true))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(categoryRepository).searchCategories("", true, pageable);
    }

    @Test
    @WithMockUser(authorities = "CATALOG_MANAGE")
    void getCategories_withInactiveFilterAndSearch_shouldNormalizeKeywordAndApplyMultipleSorts() throws Exception {
        Pageable pageable = PageRequest.of(1, 2, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.asc("name"), Sort.Order.asc("id")
        ));
        Category category = new Category("Archived phones", "archived-phones", null);
        category.deactivate();
        when(categoryRepository.searchCategories("phone", false, pageable))
                .thenReturn(new PageImpl<>(List.of(category), pageable, 3));

        mvc.perform(get("/api/v1/categories")
                        .param("keyword", "  PhOnE  ")
                        .param("active", "false")
                        .param("currentPage", "1")
                        .param("pageSize", "2")
                        .param("sortBy", "createdAt:DESC,name:asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].active").value(false))
                .andExpect(jsonPath("$.data.currentPage").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        verify(categoryRepository).searchCategories("phone", false, pageable);
    }

    @Test
    void getCategories_withInactiveFilterAnonymously_shouldForbidAccess() throws Exception {
        mvc.perform(get("/api/v1/categories").param("active", "false"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryRepository);
    }

    @Test
    @WithMockUser
    void getCategories_withInactiveFilterWithoutPermission_shouldForbidAccess() throws Exception {
        mvc.perform(get("/api/v1/categories").param("active", "false"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "currentPage,-1",
            "currentPage,abc",
            "pageSize,0",
            "pageSize,101",
            "active,invalid",
            "sortBy,name:invalid",
            "sortBy,products:asc",
            "sortBy,products.name:asc"
    })
    void getCategories_withInvalidParameter_shouldReturnBadRequest(String parameter, String value) throws Exception {
        mvc.perform(get("/api/v1/categories").param(parameter, value))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryRepository);
    }

    @Test
    void getCategories_withoutMatches_shouldReturnEmptyPage() throws Exception {
        when(categoryRepository.searchCategories(anyString(), anyBoolean(), any(Pageable.class)))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(2, Pageable.class)));

        mvc.perform(get("/api/v1/categories").param("keyword", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @Test
    void getCategories_withWildcardCharacters_shouldSearchLiterally() throws Exception {
        when(categoryRepository.searchCategories(anyString(), anyBoolean(), any(Pageable.class)))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(2, Pageable.class)));

        mvc.perform(get("/api/v1/categories").param("keyword", "50%_OFF!"))
                .andExpect(status().isOk());

        verify(categoryRepository).searchCategories(eq("50!%!_off!!"), eq(true), any(Pageable.class));
    }
}
