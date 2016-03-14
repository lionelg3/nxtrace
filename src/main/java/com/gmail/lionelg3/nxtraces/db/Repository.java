package com.gmail.lionelg3.nxtraces.db;

import com.gmail.lionelg3.nxtraces.engine.NXTraces;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.jboss.logging.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Collect mail traces from Unix systems
 *
 * @author Lionel G (lionelg3@gmail.com)
 *         Date: 26/12/15, Time: 09:59
 */
public class Repository {

    private static final Logger LOGGER = Logger.getLogger(Repository.class);
    private static final Repository _REPOSITORY = new Repository();

    private DefaultCacheManager cacheManager;
    private Cache<Object, Object> database;
    private SearchManager searchManager;

    private String filesRelativePath;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");

    private Repository() {
        super();
        this.filesRelativePath = NXTraces.CONFIGURATION.getString("repository.path");
        LOGGER.debug("Using repository filesRelativePath " + this.filesRelativePath);
        if (!new File(filesRelativePath).exists() && !new File(filesRelativePath).mkdir()) {
            LOGGER.error("Create " + this.filesRelativePath + " failed !!!");
            System.exit(-1);
        }
        try {
            this.cacheManager = new DefaultCacheManager(NXTraces.CONFIGURATION.getString("database.configuration"));
        } catch (IOException e) {
            LOGGER.error("Infinispan load default cache manager failled " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        this.database = cacheManager.getCache();
        this.searchManager = Search.getSearchManager(database);
        this.searchManager.getMassIndexer().start();
    }

    public static Repository singleton() {
        return _REPOSITORY;
    }

    public void insertMessage(String id, byte[] data) throws IOException {
        String subpath = df.format(new Date());
        String filename = subpath + File.separator + id;
        String fullpath = this.filesRelativePath + File.separator + subpath;
        if (new File(fullpath).exists() || new File(fullpath).mkdirs()) {
            FileOutputStream fout = new FileOutputStream(new File(this.filesRelativePath + File.separator + filename));
            fout.write(data);
            fout.flush();
            fout.close();
            LOGGER.info("Insert: " + filename);
            database.put(filename, new LogMessage(subpath, id));
        } else {
            throw new IOException("Can not create directory in " + this.filesRelativePath);
        }
    }

    public LogMessage load(String id) {
        QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(LogMessage.class).get();
        Query findById = queryBuilder
                .bool()
                .must(
                        queryBuilder
                                .phrase()
                                .onField("id")
                                .sentence(id)
                                .createQuery()
                ).createQuery();
        LOGGER.info("FindById: " + findById);

        CacheQuery cacheQuery = searchManager.getQuery(findById, LogMessage.class).maxResults(1);

        List<Object> list = cacheQuery.list();
        return (list != null && list.size() == 1) ? (LogMessage) list.get(0) : null;
    }

    public Result searchMessage(ArrayList<String[]> constraints, int max, int first) {
        QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(LogMessage.class).get();
        BooleanJunction<BooleanJunction> search = queryBuilder.bool();

        LOGGER.info("Search: max=" + max + ", first=" + first + ",");

        constraints.forEach(strings -> {
            String type = strings[0];
            String key = strings[1];
            String value = (strings.length > 2) ? strings[2] : null;
            LOGGER.info("    type=" + type + ", key=" + key + ", value=" + value);
            switch (type) {
                case "equals": // {"equals", "from", "lionelg3@gmail.com"}
                {
                    search.must(
                            queryBuilder
                                    .keyword()
                                    .onField(key)
                                    .matching(value)
                                    .createQuery()
                    );
                } break;
                case "start with": // {"start with", "subject", "[SSH]"}
                {
                    search.must(
                            queryBuilder
                                    .keyword()
                                    .wildcard()
                                    .onField(key)
                                    .matching(value + "*")
                                    .createQuery()
                    );
                } break;
                case "end with": // {"end with", "subject", "error"}
                {
                    search.must(
                            queryBuilder
                                    .keyword()
                                    .wildcard()
                                    .onField(key)
                                    .matching("*" + value)
                                    .createQuery()
                    );
                } break;
                case "containts": // {"containts", "content", "update"}
                {
                    search.must(
                            queryBuilder
                                    .keyword()
                                    .wildcard()
                                    .onField(key)
                                    .matching("*" + value + "*")
                                    .createQuery()
                    );
                } break;
                case "on day": // {"on date", "2016,3,4"}
                {
                    BooleanJunction<BooleanJunction> inverval = queryBuilder.bool();

                    LocalDate today = parseDate(key);

                    LocalDateTime start = today.atTime(0, 0, 0);
                    LocalDateTime stop = today.atTime(23, 59, 59);

                    Date begin = Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
                    Date end = Date.from(stop.atZone(ZoneId.systemDefault()).toInstant());

                    inverval.must(
                            queryBuilder
                                    .range()
                                    .onField("sentDate")
                                    .above(begin)
                                    .createQuery()
                    );
                    inverval.must(
                            queryBuilder
                                    .range()
                                    .onField("sentDate")
                                    .below(end)
                                    .createQuery()
                    );
                    search.must(inverval.createQuery());
                } break;
                case "between days": // {"between dates", "2016,3,4", "2016,3,5"}
                {
                    BooleanJunction<BooleanJunction> inverval = queryBuilder.bool();

                    LocalDate beginDay = parseDate(key);
                    LocalDate endDay = parseDate(value);

                    LocalDateTime start = beginDay.atTime(0, 0, 0);
                    LocalDateTime stop = endDay.atTime(23, 59, 59);

                    Date begin = Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
                    Date end = Date.from(stop.atZone(ZoneId.systemDefault()).toInstant());

                    inverval.must(
                            queryBuilder
                                    .range()
                                    .onField("sentDate")
                                    .above(begin)
                                    .createQuery()
                    );
                    inverval.must(
                            queryBuilder
                                    .range()
                                    .onField("sentDate")
                                    .below(end)
                                    .createQuery()
                    );
                    search.must(inverval.createQuery());
                } break;
            }
        });

        LOGGER.info("    query " + search.createQuery());

        CacheQuery cacheQuery = searchManager.getQuery(search.createQuery(), LogMessage.class)
                .maxResults(max)
                .firstResult(first);

        Result result = new Result();
        result.count = cacheQuery.getResultSize();
        result.list = cacheQuery.list();
        return result;
    }

    private LocalDate parseDate(String d) {
        LocalDate localDate = null;
        String[] ar = d.split(",");
        try {
            localDate = LocalDate.of(Integer.parseInt(ar[0]), Integer.parseInt(ar[1]), Integer.parseInt(ar[2]));
        } catch (RuntimeException e) {
            e.printStackTrace();
            LOGGER.warn("Error date format should be \"2016,3,5\" found : " + d);
        }
        return localDate;
    }

    public class Result {
        private int count;
        private List<Object> list;

        public int getCount() {
            return count;
        }

        public List<Object> getList() {
            return list;
        }
    }
}
