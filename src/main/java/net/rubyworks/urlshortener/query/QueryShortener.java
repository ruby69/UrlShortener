package net.rubyworks.urlshortener.query;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import net.rubyworks.urlshortener.query.domain.QShorten;
import net.rubyworks.urlshortener.query.domain.QShortenRepository;

@RequiredArgsConstructor
@Component
public class QueryShortener {
    private final QShortenRepository shortenRepository;

    public List<QShorten> findAll() {
        var findAll = shortenRepository.findAll();
        return StreamSupport.stream(findAll.spliterator(), true).toList();
    }

    public List<QShorten> findByDuration(String duration) {
        var timeMillis = timeMillis(duration);
        var list = findAll().stream()
                .filter(it -> !it.isFixed() && it.getModifiedAt() > timeMillis)
                .sorted(Comparator.comparing(QShorten::getModifiedAt).reversed())
                .toList();

        return StreamSupport.stream(list.spliterator(), true).toList();
    }

    private long timeMillis(String duration) {
        var last = duration.substring(duration.length() - 1);
        var period = Long.parseLong(duration.substring(0, duration.length() - 1));

        var now = LocalDateTime.now();
        LocalDateTime time = null;
        if ("h".equals(last)) {         // hours
            time = now.minusHours(period);
        } else if ("d".equals(last)) {  // days
            time = now.minusDays(period);
        } else if ("w".equals(last)) {  // weeks
            time = now.minusWeeks(period);
        } else {
            time = now.minusMinutes(period); // minutes
        }
        return ZonedDateTime.of(time, ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
