package net.rubyworks.urlshortener.domain;

import java.io.Serializable;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("shorten")
@JsonInclude(Include.NON_NULL)
public class Shorten implements Serializable {
    private static final long serialVersionUID = -82660867274461708L;

    @Id private String id;
    private String url;
    private long count;
    @TimeToLive(unit = TimeUnit.SECONDS) private long ttl;
    private long modifiedAt;
    private boolean fixed;

    public String getBan() {
        return "https://dii.im/api/delete/" + id;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getModifiedTime() {
        return new Date(modifiedAt);
    }

    public URI uri() {
        return URI.create(url);
    }

    public Map<String, String> url(String base) {
        return Map.of("to", base + "/" + id);
    }

    public static Shorten of(String id, String url) {
        return of(id, url, -1, -1, true);
    }

    public static Shorten of(String id, String url, long count, long ttl, boolean fixed) {
        return builder()
                .id(id)
                .url(url)
                .count(count)
                .ttl(ttl)
                .modifiedAt(Instant.now().toEpochMilli())
                .fixed(fixed)
                .build();
    }

    public static Shorten copy(Shorten shorten, long count) {
        return of(shorten.id, shorten.url, count, shorten.ttl, shorten.fixed);
    }

    public static Shorten copy(Shorten shorten) {
        return of(shorten.id, shorten.url, shorten.count, shorten.ttl, shorten.fixed);
    }

    public static String random(int count) {
        return RandomStringUtils.randomAlphanumeric(count);
    }

    private static final UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });
    public static record Param(String id, String url, long count, long ttl) {
        public boolean isValid() {
            return urlValidator.isValid(url);
        }
    }
}
