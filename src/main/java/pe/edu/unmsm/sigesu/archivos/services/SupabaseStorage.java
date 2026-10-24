package pe.edu.unmsm.sigesu.archivos.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pe.edu.unmsm.sigesu.common.*;

@Service
public class SupabaseStorage implements EvidenceStorage {
  private final String base, key, bucket;
  private final int ttl;
  private final ObjectMapper json;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public SupabaseStorage(
      @Value("${app.supabase.url}") String base,
      @Value("${app.supabase.service-key}") String key,
      @Value("${app.supabase.bucket}") String bucket,
      @Value("${app.supabase.signed-url-seconds}") int ttl,
      ObjectMapper json) {
    this.base = base.replaceAll("/$", "");
    this.key = key;
    this.bucket = bucket;
    this.ttl = ttl;
    this.json = json;
  }

  private String encoded(String s) {
    return Arrays.stream(s.split("/"))
        .map(v -> URLEncoder.encode(v, StandardCharsets.UTF_8).replace("+", "%20"))
        .collect(java.util.stream.Collectors.joining("/"));
  }

  private HttpResponse<String> call(String method, String path, String mime, byte[] bytes) {
    if (base.isBlank() || key.isBlank())
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE, "El almacenamiento de archivos no está configurado.");
    try {
      var request =
          HttpRequest.newBuilder(URI.create(base + "/storage/v1/" + path))
              .timeout(Duration.ofSeconds(30))
              .header("Authorization", "Bearer " + key)
              .header("apikey", key)
              .header("Content-Type", mime)
              .method(
                  method,
                  bytes == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofByteArray(bytes))
              .build();
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new ApiException(
            HttpStatus.BAD_GATEWAY, "No se pudo completar la operación de almacenamiento.");
      return response;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE, "Almacenamiento temporalmente no disponible.");
    } catch (java.io.IOException e) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "No se pudo conectar al almacenamiento.");
    }
  }

  @Override
  public void upload(String path, String mime, byte[] bytes) {
    call("POST", "object/" + encoded(bucket) + "/" + encoded(path), mime, bytes);
  }

  @Override
  public void delete(String path) {
    try {
      call(
          "DELETE",
          "object/" + encoded(bucket),
          "application/json",
          json.writeValueAsBytes(Map.of("prefixes", List.of(path))));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String signedUrl(String path) {
    try {
      var response =
          call(
              "POST",
              "object/sign/" + encoded(bucket) + "/" + encoded(path),
              "application/json",
              json.writeValueAsBytes(Map.of("expiresIn", ttl)));
      String url = json.readTree(response.body()).path("signedURL").asText();
      if (!url.startsWith("/object/sign/"))
        throw new ApiException(HttpStatus.BAD_GATEWAY, "Respuesta de almacenamiento inválida.");
      return base + "/storage/v1" + url;
    } catch (java.io.IOException e) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Respuesta de almacenamiento inválida.");
    }
  }

  @Override
  public int ttl() {
    return ttl;
  }
}
