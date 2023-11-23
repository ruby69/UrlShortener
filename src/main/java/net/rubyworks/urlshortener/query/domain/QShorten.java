package net.rubyworks.urlshortener.query.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@RedisHash("shorten")
@JsonInclude(Include.NON_NULL)
public class QShorten implements Serializable {
    private static final long serialVersionUID = 5661465184955866502L;

    @Id private String id;
    private String url;
    private long count;
    @TimeToLive(unit = TimeUnit.SECONDS) private long ttl;
    private long modifiedAt;
    private boolean fixed;

    public String getBan() {
        return "https://s.dii.im/api/delete/" + id;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getModifiedTime() {
        return new Date(modifiedAt);
    }

}
