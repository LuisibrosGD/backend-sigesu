package pe.edu.unmsm.sigesu.common;

import java.util.*;
import org.springframework.data.domain.*;

/** Shared wire pagination, with the original defaults and validation. */
public final class ReadPages {
  private ReadPages() {}

  public static int number(Map<String, String> f, String k, int fallback, int max) {
    try {
      return Math.min(
          max,
          Math.max(k.equals("page") ? 0 : 1, Integer.parseInt(f.getOrDefault(k, "" + fallback))));
    } catch (NumberFormatException e) {
      throw ApiException.bad("Paginación inválida.");
    }
  }

  public static Pageable page(Map<String, String> f) {
    return PageRequest.of(number(f, "page", 0, 1000000), number(f, "size", 10, 100));
  }

  public static Map<String, Object> envelope(Page<?> p, Object content) {
    return Map.of(
        "content",
        content,
        "page",
        p.getNumber(),
        "size",
        p.getSize(),
        "totalElements",
        p.getTotalElements());
  }
}
