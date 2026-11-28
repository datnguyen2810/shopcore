package io.github.datnguyen2810.shopcore;

import io.github.datnguyen2810.shopcore.category.Category;
import io.github.datnguyen2810.shopcore.category.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CategoryRepositoryTests {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void searchCategories_withKeywordAndActive_shouldFilterBeforePagingAndCountMatches() {
        categoryRepository.save(new Category("Phones B", "search-phones-b", null));
        categoryRepository.save(new Category("Phones A", "search-phones-a", null));
        Category inactive = new Category("Phones archived", "search-phones-archived", null);
        inactive.deactivate();
        categoryRepository.save(inactive);
        categoryRepository.save(new Category("Unrelated", "search-unrelated", null));

        var pageable = PageRequest.of(0, 1, Sort.by("name", "id"));
        var activePage = categoryRepository.searchCategories("phones", true, pageable);
        var nextPage = categoryRepository.searchCategories("phones", true, pageable.next());
        var inactivePage = categoryRepository.searchCategories("phones", false, pageable);

        assertThat(activePage.getContent()).extracting(Category::getName).containsExactly("Phones A");
        assertThat(activePage.getTotalElements()).isEqualTo(2);
        assertThat(activePage.getTotalPages()).isEqualTo(2);
        assertThat(nextPage.getContent()).extracting(Category::getName).containsExactly("Phones B");
        assertThat(inactivePage.getContent()).extracting(Category::getId).containsExactly(inactive.getId());
        assertThat(inactivePage.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchCategories_withSlugOrEscapedKeyword_shouldMatchCaseInsensitivelyAndLiterally() {
        Category slugMatch = categoryRepository.save(new Category("Accessories", "search-phone-cases", null));
        Category literalMatch = categoryRepository.save(new Category("50%_OFF!", "search-sale", null));
        categoryRepository.save(new Category("50xxOFF!", "search-other-sale", null));
        var pageable = PageRequest.of(0, 20, Sort.by("name", "id"));

        assertThat(categoryRepository.searchCategories("phone-cases", true, pageable).getContent())
                .extracting(Category::getId).containsExactly(slugMatch.getId());
        assertThat(categoryRepository.searchCategories("50!%!_off!!", true, pageable).getContent())
                .extracting(Category::getId).containsExactly(literalMatch.getId());
        assertThat(categoryRepository.searchCategories("does-not-exist", true, pageable).getContent()).isEmpty();
    }
}
