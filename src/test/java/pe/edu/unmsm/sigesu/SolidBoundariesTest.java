package pe.edu.unmsm.sigesu;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.*;
import org.junit.jupiter.api.Test;
import pe.edu.unmsm.sigesu.archivos.services.*;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.*;

class SolidBoundariesTest {
  @Test
  void catalogServiceDoesNotExposeRepositories() {
    for (var field : CatalogosService.class.getDeclaredFields())
      assertTrue(Modifier.isPrivate(field.getModifiers()));
    for (var method : CatalogosService.class.getDeclaredMethods())
      assertFalse(
          org.springframework.data.repository.Repository.class.isAssignableFrom(
              method.getReturnType()));
  }

  @Test
  void storageConsumersDependOnPort() {
    assertTrue(EvidenceStorage.class.isAssignableFrom(SupabaseStorage.class));
    for (var consumer :
        List.of(
            ArchivosService.class,
            pe.edu.unmsm.sigesu.application.services.DevResetService.class)) {
      var parameters = Arrays.asList(consumer.getConstructors()[0].getParameterTypes());
      assertTrue(parameters.contains(EvidenceStorage.class));
      assertFalse(parameters.contains(SupabaseStorage.class));
    }
  }

  @Test
  void sharedPaginationKeepsDefaultsBoundsAndWireShape() {
    var initial = ReadPages.page(Map.of());
    assertEquals(0, initial.getPageNumber());
    assertEquals(10, initial.getPageSize());
    assertEquals(100, ReadPages.page(Map.of("size", "1000")).getPageSize());
    assertEquals(0, ReadPages.page(Map.of("page", "-1")).getPageNumber());
    var failure = assertThrows(ApiException.class, () -> ReadPages.page(Map.of("page", "bad")));
    assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, failure.status);
    var page = new org.springframework.data.domain.PageImpl<>(List.of("item"), initial, 1);
    assertEquals(
        Set.of("content", "page", "size", "totalElements"),
        ReadPages.envelope(page, page.getContent()).keySet());
  }
}
