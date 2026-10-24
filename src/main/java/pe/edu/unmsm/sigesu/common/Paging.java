package pe.edu.unmsm.sigesu.common;

import java.time.*;
import java.util.*;

public final class Paging {
  public static List<Map<String, Object>> filter(
      List<Map<String, Object>> items, Map<String, String> filters) {
    return items.stream()
        .filter(
            row ->
                filters.entrySet().stream()
                    .allMatch(
                        e -> {
                          String k = e.getKey(), v = e.getValue();
                          if (v.isBlank()
                              || v.equals("ALL")
                              || Set.of("page", "size", "sort", "order").contains(k)) return true;
                          if (k.equals("search"))
                            return (""
                                    + row.get("titulo")
                                    + row.get("codigo")
                                    + row.get("descripcion"))
                                .toLowerCase(Locale.ROOT)
                                .contains(v.toLowerCase(Locale.ROOT));
                          if (k.equals("fechaDesde") || k.equals("fechaHasta")) {
                            String date =
                                String.valueOf(row.getOrDefault("fechaRegistro", row.get("fecha")))
                                    .substring(0, 10);
                            return k.equals("fechaDesde")
                                ? date.compareTo(v) >= 0
                                : date.compareTo(v) <= 0;
                          }
                          return !row.containsKey(k)
                              || Objects.equals(String.valueOf(row.get(k)), v);
                        }))
        .toList();
  }

  public static Map<String, Object> page(
      List<Map<String, Object>> input, Map<String, String> filters) {
    var data = new ArrayList<>(filter(input, filters));
    int page = Math.max(0, Integer.parseInt(filters.getOrDefault("page", "0"))),
        size = Math.min(100, Math.max(1, Integer.parseInt(filters.getOrDefault("size", "10"))));
    String sort = filters.getOrDefault("sort", "fechaRegistro");
    Comparator<Map<String, Object>> c =
        Comparator.comparing(row -> String.valueOf(row.getOrDefault(sort, "")));
    if (!filters.getOrDefault("order", "desc").equals("asc")) c = c.reversed();
    data.sort(c);
    int start = (int) Math.min((long) page * size, data.size());
    return Map.of(
        "content",
        data.subList(start, Math.min(start + size, data.size())),
        "page",
        page,
        "size",
        size,
        "totalElements",
        data.size());
  }
}
