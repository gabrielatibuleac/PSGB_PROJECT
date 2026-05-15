SET SERVEROUTPUT ON;
ALTER SESSION SET PLSCOPE_SETTINGS = 'IDENTIFIERS:NONE';

-- ==========================================================
-- 1. CURĂȚENIE TOTALĂ
-- ==========================================================
BEGIN
    FOR r IN (SELECT table_name FROM user_tables WHERE table_name IN 
        ('WATCH_HISTORY', 'MOVIE_RATINGS', 'MOVIE_COMMENTS', 'WATCHLIST', 
         'MOVIE_FORMATS', 'MOVIE_ACTORS', 'MOVIE_DIRECTORS', 'MOVIES', 
         'ACTORS', 'DIRECTORS', 'USERS', 'CATEGORIES')) 
    LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || r.table_name || ' CASCADE CONSTRAINTS';
    END LOOP;
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    FOR s IN (SELECT sequence_name FROM user_sequences WHERE sequence_name IN 
        ('SEQ_CATEGORIES','SEQ_USERS','SEQ_MOVIES','SEQ_MOVIE_FORMATS',
         'SEQ_MOVIE_COMMENTS','SEQ_DIRECTORS','SEQ_ACTORS','SEQ_RATINGS')) 
    LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
    END LOOP;
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TYPE T_RECOMMENDATION_TABLE';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TYPE T_RECOMMENDATION_ROW';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- ==========================================================
-- 2. SECVENȚE
-- ==========================================================
CREATE SEQUENCE SEQ_CATEGORIES  START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_USERS       START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_MOVIES      START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_DIRECTORS   START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_ACTORS      START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_MOVIE_FORMATS  START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_MOVIE_COMMENTS START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_RATINGS     START WITH 1 INCREMENT BY 1 NOCACHE;

-- ==========================================================
-- 3. TABELE
-- ==========================================================
CREATE TABLE categories (
    id   NUMBER PRIMARY KEY,
    name VARCHAR2(100) NOT NULL UNIQUE
);

CREATE TABLE users (
    id            NUMBER PRIMARY KEY,
    full_name     VARCHAR2(255) NOT NULL,
    email         VARCHAR2(255) NOT NULL UNIQUE,
    password_hash VARCHAR2(255) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE directors (
    id         NUMBER PRIMARY KEY,
    first_name VARCHAR2(100) NOT NULL,
    last_name  VARCHAR2(100) NOT NULL,
    photo_url  VARCHAR2(500)
);

CREATE TABLE actors (
    id         NUMBER PRIMARY KEY,
    first_name VARCHAR2(100) NOT NULL,
    last_name  VARCHAR2(100) NOT NULL,
    photo_url  VARCHAR2(500)
);

CREATE TABLE movies (
    id           NUMBER PRIMARY KEY,
    title        VARCHAR2(255) NOT NULL UNIQUE,
    release_year NUMBER(4) NOT NULL,
    id_category  NUMBER NOT NULL REFERENCES categories(id),
    poster_url   VARCHAR2(500),
    description  CLOB,
    trailer_url  VARCHAR2(500),
    view_count   NUMBER DEFAULT 0
);

CREATE TABLE movie_formats (
    id            NUMBER PRIMARY KEY,
    id_movie      NUMBER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    format_name   VARCHAR2(100) NOT NULL,
    quality_label VARCHAR2(50),
    source_link   VARCHAR2(500)
);

CREATE TABLE movie_directors (
    id_movie    NUMBER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    id_director NUMBER NOT NULL REFERENCES directors(id),
    PRIMARY KEY (id_movie, id_director)
);

CREATE TABLE movie_actors (
    id_movie  NUMBER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    id_actor  NUMBER NOT NULL REFERENCES actors(id),
    role_name VARCHAR2(100),
    PRIMARY KEY (id_movie, id_actor)
);

CREATE TABLE watch_history (
    id_user    NUMBER NOT NULL REFERENCES users(id),
    id_movie   NUMBER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    id_format  NUMBER REFERENCES movie_formats(id),
    watch_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_user, id_movie, watch_date)
);

CREATE TABLE movie_ratings (
    id       NUMBER PRIMARY KEY,
    id_user  NUMBER NOT NULL REFERENCES users(id),
    id_movie NUMBER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    rating   NUMBER(2,1) NOT NULL,
    rated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_user, id_movie),
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 10)
);

CREATE TABLE movie_comments (
    id           NUMBER PRIMARY KEY,
    id_user      NUMBER NOT NULL REFERENCES users(id),
    id_movie     NUMBER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    comment_text CLOB NOT NULL,
    posted_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE watchlist (
    id_user  NUMBER NOT NULL REFERENCES users(id),
    id_movie NUMBER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_user, id_movie)
);

-- ==========================================================
-- 4. TRIGGERE
-- ==========================================================
CREATE OR REPLACE TRIGGER trg_increment_view_count
AFTER INSERT ON watch_history
FOR EACH ROW
BEGIN
    UPDATE movies SET view_count = view_count + 1 WHERE id = :NEW.id_movie;
END;
/

CREATE OR REPLACE TRIGGER trg_validate_rating_change
BEFORE UPDATE ON movie_ratings
FOR EACH ROW
BEGIN
    IF ABS(:NEW.rating - :OLD.rating) > 5 THEN
        RAISE_APPLICATION_ERROR(-20010,
            'Modificarea rating-ului nu poate depasi 5 puncte. '||
            'Rating curent: ' || :OLD.rating || ', Rating nou: ' || :NEW.rating);
    END IF;
    :NEW.rated_at := CURRENT_TIMESTAMP;
    DBMS_OUTPUT.PUT_LINE('Rating modificat user ' || :NEW.id_user ||
                         ' film ' || :NEW.id_movie ||
                         ': ' || :OLD.rating || ' -> ' || :NEW.rating);
END;
/

-- TRIGGER 3: folosim DBMS_LOB.COMPARE pentru CLOB (fix pentru Oracle XE 11g)
CREATE OR REPLACE TRIGGER trg_prevent_duplicate_comment
BEFORE INSERT ON movie_comments
FOR EACH ROW
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM movie_comments
    WHERE id_user = :NEW.id_user
      AND id_movie = :NEW.id_movie
      AND DBMS_LOB.COMPARE(comment_text, :NEW.comment_text) = 0
      AND posted_at > CURRENT_TIMESTAMP - INTERVAL '10' SECOND;

    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20020,
            'Comentariu duplicat detectat. Asteptati inainte de a reposta.');
    END IF;
END;
/

-- ==========================================================
-- 5. TIPURI OBIECT
-- ==========================================================
CREATE OR REPLACE TYPE T_RECOMMENDATION_ROW AS OBJECT (
    movie_id      NUMBER,
    movie_title   VARCHAR2(255),
    release_year  NUMBER,
    category_name VARCHAR2(100),
    score         NUMBER,
    avg_rating    NUMBER,
    view_count    NUMBER,
    poster_url    VARCHAR2(500)
);
/

CREATE OR REPLACE TYPE T_RECOMMENDATION_TABLE AS TABLE OF T_RECOMMENDATION_ROW;
/

-- ==========================================================
-- 6. PACHET
-- ==========================================================
CREATE OR REPLACE PACKAGE cinematheque_pkg AS

    ex_user_not_found       EXCEPTION;
    ex_movie_not_found      EXCEPTION;
    ex_already_in_watchlist EXCEPTION;
    ex_invalid_rating       EXCEPTION;
    ex_self_duplicate_watch EXCEPTION;

    PRAGMA EXCEPTION_INIT(ex_user_not_found,       -20001);
    PRAGMA EXCEPTION_INIT(ex_movie_not_found,       -20002);
    PRAGMA EXCEPTION_INIT(ex_already_in_watchlist,  -20003);
    PRAGMA EXCEPTION_INIT(ex_invalid_rating,        -20004);

    PROCEDURE register_user(
        p_full_name     IN users.full_name%TYPE,
        p_email         IN users.email%TYPE,
        p_password_hash IN users.password_hash%TYPE,
        p_new_id        OUT users.id%TYPE
    );

    PROCEDURE login_user(
        p_email         IN  users.email%TYPE,
        p_password_hash IN  users.password_hash%TYPE,
        p_user_id       OUT users.id%TYPE,
        p_full_name     OUT users.full_name%TYPE
    );

    PROCEDURE record_watch(
        p_user_id   IN watch_history.id_user%TYPE,
        p_movie_id  IN watch_history.id_movie%TYPE,
        p_format_id IN watch_history.id_format%TYPE
    );

    PROCEDURE add_to_watchlist(
        p_user_id  IN watchlist.id_user%TYPE,
        p_movie_id IN watchlist.id_movie%TYPE
    );

    PROCEDURE remove_from_watchlist(
        p_user_id  IN watchlist.id_user%TYPE,
        p_movie_id IN watchlist.id_movie%TYPE
    );

    PROCEDURE add_comment(
        p_user_id      IN movie_comments.id_user%TYPE,
        p_movie_id     IN movie_comments.id_movie%TYPE,
        p_comment_text IN movie_comments.comment_text%TYPE,
        p_comment_id   OUT movie_comments.id%TYPE
    );

    PROCEDURE rate_movie(
        p_user_id  IN movie_ratings.id_user%TYPE,
        p_movie_id IN movie_ratings.id_movie%TYPE,
        p_rating   IN movie_ratings.rating%TYPE
    );

    FUNCTION get_movie_avg_rating(p_movie_id IN NUMBER) RETURN NUMBER;
    FUNCTION get_user_favorite_category(p_user_id IN NUMBER) RETURN NUMBER;

    FUNCTION recommend_movies(
        p_user_id   IN NUMBER,
        p_max_count IN NUMBER DEFAULT 10
    ) RETURN T_RECOMMENDATION_TABLE PIPELINED;

END cinematheque_pkg;
/

-- ==========================================================
-- 7. IMPLEMENTARE PACHET
-- ==========================================================
CREATE OR REPLACE PACKAGE BODY cinematheque_pkg AS

    PROCEDURE register_user(
        p_full_name     IN users.full_name%TYPE,
        p_email         IN users.email%TYPE,
        p_password_hash IN users.password_hash%TYPE,
        p_new_id        OUT users.id%TYPE
    ) IS
        v_existing NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_existing FROM users WHERE email = p_email;
        IF v_existing > 0 THEN
            RAISE_APPLICATION_ERROR(-20030,
                'Email-ul ' || p_email || ' este deja inregistrat in sistem.');
        END IF;
        p_new_id := SEQ_USERS.NEXTVAL;
        INSERT INTO users (id, full_name, email, password_hash)
        VALUES (p_new_id, p_full_name, p_email, p_password_hash);
        COMMIT;
    EXCEPTION
        WHEN OTHERS THEN ROLLBACK; RAISE;
    END register_user;

    PROCEDURE login_user(
        p_email         IN  users.email%TYPE,
        p_password_hash IN  users.password_hash%TYPE,
        p_user_id       OUT users.id%TYPE,
        p_full_name     OUT users.full_name%TYPE
    ) IS
    BEGIN
        SELECT id, full_name INTO p_user_id, p_full_name
        FROM users
        WHERE email = p_email AND password_hash = p_password_hash;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20001,
                'Email sau parola incorecta. Verificati credentialele.');
    END login_user;

    PROCEDURE record_watch(
        p_user_id   IN watch_history.id_user%TYPE,
        p_movie_id  IN watch_history.id_movie%TYPE,
        p_format_id IN watch_history.id_format%TYPE
    ) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM users WHERE id = p_user_id;
        IF v_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Utilizatorul cu ID ' || p_user_id || ' nu exista.');
        END IF;
        SELECT COUNT(*) INTO v_count FROM movies WHERE id = p_movie_id;
        IF v_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20002, 'Filmul cu ID ' || p_movie_id || ' nu exista.');
        END IF;
        INSERT INTO watch_history (id_user, id_movie, id_format, watch_date)
        VALUES (p_user_id, p_movie_id, p_format_id, CURRENT_TIMESTAMP);
        COMMIT;
    END record_watch;

    PROCEDURE add_to_watchlist(
        p_user_id  IN watchlist.id_user%TYPE,
        p_movie_id IN watchlist.id_movie%TYPE
    ) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM users WHERE id = p_user_id;
        IF v_count = 0 THEN RAISE_APPLICATION_ERROR(-20001, 'Utilizatorul nu exista.'); END IF;
        SELECT COUNT(*) INTO v_count FROM movies WHERE id = p_movie_id;
        IF v_count = 0 THEN RAISE_APPLICATION_ERROR(-20002, 'Filmul nu exista.'); END IF;
        SELECT COUNT(*) INTO v_count FROM watchlist
        WHERE id_user = p_user_id AND id_movie = p_movie_id;
        IF v_count > 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'Filmul este deja in lista de urmarit.');
        END IF;
        INSERT INTO watchlist (id_user, id_movie) VALUES (p_user_id, p_movie_id);
        COMMIT;
    EXCEPTION
        WHEN OTHERS THEN ROLLBACK; RAISE;
    END add_to_watchlist;

    PROCEDURE remove_from_watchlist(
        p_user_id  IN watchlist.id_user%TYPE,
        p_movie_id IN watchlist.id_movie%TYPE
    ) IS
    BEGIN
        DELETE FROM watchlist WHERE id_user = p_user_id AND id_movie = p_movie_id;
        IF SQL%ROWCOUNT = 0 THEN
            RAISE_APPLICATION_ERROR(-20005, 'Filmul nu se afla in watchlist-ul userului.');
        END IF;
        COMMIT;
    END remove_from_watchlist;

    PROCEDURE add_comment(
        p_user_id      IN movie_comments.id_user%TYPE,
        p_movie_id     IN movie_comments.id_movie%TYPE,
        p_comment_text IN movie_comments.comment_text%TYPE,
        p_comment_id   OUT movie_comments.id%TYPE
    ) IS
        v_count NUMBER;
    BEGIN
        IF p_comment_text IS NULL OR LENGTH(TRIM(p_comment_text)) = 0 THEN
            RAISE_APPLICATION_ERROR(-20006, 'Comentariul nu poate fi gol.');
        END IF;
        SELECT COUNT(*) INTO v_count FROM users WHERE id = p_user_id;
        IF v_count = 0 THEN RAISE_APPLICATION_ERROR(-20001, 'Utilizatorul nu exista.'); END IF;
        SELECT COUNT(*) INTO v_count FROM movies WHERE id = p_movie_id;
        IF v_count = 0 THEN RAISE_APPLICATION_ERROR(-20002, 'Filmul nu exista.'); END IF;
        p_comment_id := SEQ_MOVIE_COMMENTS.NEXTVAL;
        INSERT INTO movie_comments (id, id_user, id_movie, comment_text)
        VALUES (p_comment_id, p_user_id, p_movie_id, p_comment_text);
        COMMIT;
    EXCEPTION
        WHEN OTHERS THEN ROLLBACK; RAISE;
    END add_comment;

    PROCEDURE rate_movie(
        p_user_id  IN movie_ratings.id_user%TYPE,
        p_movie_id IN movie_ratings.id_movie%TYPE,
        p_rating   IN movie_ratings.rating%TYPE
    ) IS
        v_count  NUMBER;
        v_new_id NUMBER;
    BEGIN
        IF p_rating < 1 OR p_rating > 10 THEN
            RAISE_APPLICATION_ERROR(-20004,
                'Rating invalid: ' || p_rating || '. Trebuie sa fie intre 1 si 10.');
        END IF;
        SELECT COUNT(*) INTO v_count FROM users WHERE id = p_user_id;
        IF v_count = 0 THEN RAISE_APPLICATION_ERROR(-20001, 'Utilizatorul nu exista.'); END IF;
        SELECT COUNT(*) INTO v_count FROM movies WHERE id = p_movie_id;
        IF v_count = 0 THEN RAISE_APPLICATION_ERROR(-20002, 'Filmul nu exista.'); END IF;
        SELECT COUNT(*) INTO v_count FROM movie_ratings
        WHERE id_user = p_user_id AND id_movie = p_movie_id;
        IF v_count > 0 THEN
            UPDATE movie_ratings SET rating = p_rating
            WHERE id_user = p_user_id AND id_movie = p_movie_id;
        ELSE
            v_new_id := SEQ_RATINGS.NEXTVAL;
            INSERT INTO movie_ratings (id, id_user, id_movie, rating)
            VALUES (v_new_id, p_user_id, p_movie_id, p_rating);
        END IF;
        COMMIT;
    EXCEPTION
        WHEN OTHERS THEN ROLLBACK; RAISE;
    END rate_movie;

    FUNCTION get_movie_avg_rating(p_movie_id IN NUMBER) RETURN NUMBER IS
        v_avg NUMBER;
    BEGIN
        SELECT ROUND(AVG(rating), 1) INTO v_avg
        FROM movie_ratings WHERE id_movie = p_movie_id;
        RETURN NVL(v_avg, 0);
    END get_movie_avg_rating;

    FUNCTION get_user_favorite_category(p_user_id IN NUMBER) RETURN NUMBER IS
        v_cat_id NUMBER;
        v_count  NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM watch_history WHERE id_user = p_user_id;
        IF v_count = 0 THEN
            SELECT id_category INTO v_cat_id FROM (
                SELECT id_category, COUNT(*) cnt
                FROM movies GROUP BY id_category ORDER BY cnt DESC
            ) WHERE ROWNUM = 1;
            RETURN v_cat_id;
        END IF;
        SELECT id_category INTO v_cat_id FROM (
            SELECT m.id_category,
                   SUM(CASE
                       WHEN wh.watch_date > CURRENT_TIMESTAMP - INTERVAL '30' DAY THEN 2
                       WHEN wh.watch_date > CURRENT_TIMESTAMP - INTERVAL '90' DAY THEN 1
                       ELSE 0.5
                   END) AS weighted_score
            FROM watch_history wh
            JOIN movies m ON m.id = wh.id_movie
            WHERE wh.id_user = p_user_id
            GROUP BY m.id_category
            ORDER BY weighted_score DESC
        ) WHERE ROWNUM = 1;
        RETURN v_cat_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN RETURN 1;
    END get_user_favorite_category;

    FUNCTION recommend_movies(
        p_user_id   IN NUMBER,
        p_max_count IN NUMBER DEFAULT 10
    ) RETURN T_RECOMMENDATION_TABLE PIPELINED IS

        v_fav_category NUMBER;
        v_max_views    NUMBER;
        v_current_year NUMBER;
        v_count        NUMBER;

        CURSOR c_movies IS
            SELECT m.id, m.title, m.release_year, m.id_category,
                   c.name AS category_name, m.view_count, m.poster_url,
                   CASE
                       WHEN m.id_category = v_fav_category THEN 50
                       WHEN m.id_category IN (
                           SELECT DISTINCT m2.id_category
                           FROM watch_history wh2
                           JOIN movies m2 ON m2.id = wh2.id_movie
                           WHERE wh2.id_user = p_user_id
                           AND m2.id_category != v_fav_category
                       ) THEN 25
                       ELSE 0
                   END AS category_score,
                   NVL((SELECT ROUND(AVG(rating),1) FROM movie_ratings
                        WHERE id_movie = m.id), 0) AS avg_r,
                   (SELECT COUNT(*) FROM watchlist
                    WHERE id_user = p_user_id AND id_movie = m.id) AS in_watchlist
            FROM movies m
            JOIN categories c ON c.id = m.id_category
            WHERE m.id NOT IN (
                SELECT DISTINCT id_movie FROM watch_history WHERE id_user = p_user_id
            )
            ORDER BY DBMS_RANDOM.VALUE;

        v_score       NUMBER;
        v_norm_views  NUMBER;
        v_norm_rating NUMBER;

        TYPE t_rec_list IS TABLE OF T_RECOMMENDATION_ROW INDEX BY PLS_INTEGER;
        v_list t_rec_list;
        v_idx  PLS_INTEGER := 1;

    BEGIN
        SELECT COUNT(*) INTO v_count FROM users WHERE id = p_user_id;
        IF v_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Utilizatorul cu ID ' || p_user_id || ' nu exista.');
        END IF;

        v_fav_category := get_user_favorite_category(p_user_id);
        v_current_year := EXTRACT(YEAR FROM CURRENT_DATE);
        SELECT NVL(MAX(view_count), 1) INTO v_max_views FROM movies;

        FOR rec IN c_movies LOOP
            v_norm_views  := (rec.view_count / v_max_views) * 20;
            v_norm_rating := (rec.avg_r / 10) * 20;
            v_score := rec.category_score + v_norm_views + v_norm_rating
                       - (rec.in_watchlist * 5);
            IF (v_current_year - rec.release_year) < 5 THEN
                v_score := v_score + 10;
            END IF;
            v_list(v_idx) := T_RECOMMENDATION_ROW(
                rec.id, rec.title, rec.release_year, rec.category_name,
                ROUND(v_score, 2), rec.avg_r, rec.view_count, rec.poster_url
            );
            v_idx := v_idx + 1;
        END LOOP;

        DECLARE
            v_tmp T_RECOMMENDATION_ROW;
            v_n   PLS_INTEGER := v_list.COUNT;
        BEGIN
            FOR i IN 1..v_n - 1 LOOP
                FOR j IN 1..v_n - i LOOP
                    IF v_list(j).score < v_list(j+1).score THEN
                        v_tmp := v_list(j); v_list(j) := v_list(j+1); v_list(j+1) := v_tmp;
                    END IF;
                END LOOP;
            END LOOP;
        END;

        FOR k IN 1..LEAST(p_max_count, v_list.COUNT) LOOP
            PIPE ROW(v_list(k));
        END LOOP;
        RETURN;
    EXCEPTION
        WHEN OTHERS THEN RAISE;
    END recommend_movies;

END cinematheque_pkg;
/

-- ==========================================================
-- 8. POPULARE DATE
-- ==========================================================

-- CATEGORII (15)
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Drama');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Action');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Comedy');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Thriller');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Sci-Fi');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Anime');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Romance');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Fantasy');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Adventure');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Documentary');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Western');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Musical');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Mystery');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Historical');
INSERT INTO categories VALUES (SEQ_CATEGORIES.NEXTVAL, 'Noir');
COMMIT;

-- REGIZORI (20) — id 1..20
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Christopher','Nolan','../../public/nolan.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Martin','Scorsese','../../public/scorsese.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Francis Ford','Coppola','../../public/coppola.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'James','Cameron','../../public/cameron.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Makoto','Shinkai','../../public/shinkai.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Hayao','Miyazaki','../../public/miyazaki.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Todd','Phillips','../../public/phillips.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Damien','Chazelle','../../public/damien.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'David','Fincher','../../public/fincher.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Greta','Gerwig','../../public/gerwig.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Peter','Jackson','../../public/jackson.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Quentin','Tarantino','../../public/tarantino.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Frank','Darabont','../../public/darabont.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Ridley','Scott','../../public/scott.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Bong','Joon-ho','../../public/bong.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Denis','Villeneuve','../../public/villeneuve.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Steven','Spielberg','../../public/spielberg.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Alastair','Fothergill','../../public/fothergill.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Sergio','Leone','../../public/leone.jpg');
INSERT INTO directors VALUES (SEQ_DIRECTORS.NEXTVAL, 'Michael','Gracey','../../public/gracey.jpg');
COMMIT;

-- ACTORI (20) — id 1..20
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Leonardo','DiCaprio','../../public/dicaprio.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Brad','Pitt','../../public/pitt.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Margot','Robbie','../../public/robbie.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Matthew','McConaughey','../../public/matthew.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Joaquin','Phoenix','../../public/phoenix.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Anne','Hathaway','../../public/anne.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Marlon','Brando','../../public/brando.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Emma','Stone','../../public/stone.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Ryan','Gosling','../../public/gosling.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Christian','Bale','../../public/bale.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Morgan','Freeman','../../public/morgan.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Tom','Hanks','../../public/tom.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Meryl','Streep','../../public/streep.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Robert','De Niro','../../public/deniro.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Scarlett','Johansson','../../public/johansson.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Elijah','Wood','../../public/wood.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Harrison','Ford','../../public/ford.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'David','Attenborough','../../public/attenborough.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Clint','Eastwood','../../public/eastwood.jpg');
INSERT INTO actors VALUES (SEQ_ACTORS.NEXTVAL, 'Hugh','Jackman','../../public/jackman.jpg');
COMMIT;

-- ==============================================================
-- FILME (24 filme - ID-uri 1..24, consistente cu scriptul tau)
-- Harta ID-uri:
--   1=Godfather, 2=Whiplash, 3=Pianist
--   4=Inception, 5=Mad Max
--   6=Hangover, 7=Barbie
--   8=Joker, 9=Shutter Island, 10=Se7en
--   11=Interstellar, 12=Blade Runner 2049
--   13=Your Name, 14=Spirited Away, 15=A Silent Voice
--   16=Titanic, 17=La La Land
--   18=Lord of the Rings
--   19=Raiders of the Lost Ark
--   20=Our Planet
--   21=Good Bad Ugly
--   22=Greatest Showman
--   23=Knives Out
--   24=Oppenheimer
-- ==============================================================
INSERT INTO movies VALUES(1,'The Godfather',1972,1,'../../public/poster1.jpg','Povestea familiei Corleone, o saga despre putere si loialitate.','https://www.youtube.com/embed/sY1S34973zA',0);
INSERT INTO movies VALUES(2,'Whiplash',2014,1,'../../public/poster5.jpg','Obsesia unui tobosár tanar pentru perfectiune absoluta.','https://www.youtube.com/embed/7d_jQycdQGo',0);
INSERT INTO movies VALUES(3,'The Pianist',2002,1,'../../public/poster4.jpg','Supravietuirea unui muzician evreu in Varsovia celui de-Al Doilea Razboi Mondial.','https://www.youtube.com/embed/BFwGqLa_oAo',0);
INSERT INTO movies VALUES(4,'Inception',2010,2,'../../public/poster0.jpg','Un hot care fura secrete din subconstient primeste o misiune imposibila.','https://www.youtube.com/embed/YoHD9XEInc0',0);
INSERT INTO movies VALUES(5,'Mad Max: Fury Road',2015,2,'../../public/poster_madmax.jpg','Urmariri spectaculoase in desert intr-o lume post-apocaliptica.','https://www.youtube.com/embed/hEJnMQG9ev8',0);
INSERT INTO movies VALUES(6,'The Hangover',2009,3,'../../public/hangover.jpg','Trei prieteni se trezesc in Las Vegas fara amintiri despre noaptea anterioara.','https://www.youtube.com/embed/tcdUjAy639w',0);
INSERT INTO movies VALUES(7,'Barbie',2023,3,'../../public/barbie.jpg','Barbie traieste o criza existentiala in lumea reala.','https://www.youtube.com/embed/pBk4NYhWNMM',0);
INSERT INTO movies VALUES(8,'Joker',2019,4,'../../public/poster_joker.jpg','Originea infamului antagonist al lui Batman.','https://www.youtube.com/embed/zAGVQLHvwOY',0);
INSERT INTO movies VALUES(9,'Shutter Island',2010,4,'../../public/poster_shutter.jpg','Un marshal federal investigheaza disparitia unui pacient de pe o insula.','https://www.youtube.com/embed/5iaYLCmq56E',0);
INSERT INTO movies VALUES(10,'Se7en',1995,4,'../../public/se7en.jpg','Doi detectivi urmaresc un criminal care ucide bazat pe cele 7 pacate capitale.','https://www.youtube.com/embed/znmZoVkCjpI',0);
INSERT INTO movies VALUES(11,'Interstellar',2014,5,'../../public/poster0.jpg','Astronauti calatoresc prin gauri de vierme pentru a salva omenirea.','https://www.youtube.com/embed/zSWdZVtXT7E',0);
INSERT INTO movies VALUES(12,'Blade Runner 2049',2017,5,'../../public/bladerunner.jpg','Un nou vanator de replicanti descopera un secret care ar putea schimba societatea.','https://www.youtube.com/embed/gCcx85zbxz4',0);
INSERT INTO movies VALUES(13,'Your Name',2016,6,'../../public/yourname.jpg','Doi adolescenti isi schimba corpurile misterios si se cauta prin timp.','https://www.youtube.com/embed/hRfHcp2G690',0);
INSERT INTO movies VALUES(14,'Spirited Away',2001,6,'../../public/poster3.jpg','O fetita ratacita in lumea spiritelor trebuie sa lucreze pentru a-si salva parintii.','https://www.youtube.com/embed/ByXuk9QqQkk',0);
INSERT INTO movies VALUES(15,'A Silent Voice',2016,6,'../../public/silentvoice.jpg','Un baiat incearca sa-si ceara iertare fetei pe care a intimidat-o.','https://www.youtube.com/embed/nfK6UgLra7g',0);
INSERT INTO movies VALUES(16,'Titanic',1997,7,'../../public/titanic.jpg','O poveste de dragoste imposibila pe nava condamnata.','https://www.youtube.com/embed/kVrqfYjknUA',0);
INSERT INTO movies VALUES(17,'La La Land',2016,7,'../../public/lalaland.jpg','Doi artisti care viseaza la Hollywood se indragostesc si se pierd unul pe altul.','https://www.youtube.com/embed/0pdqf4P9MB8',0);
INSERT INTO movies VALUES(18,'The Lord of the Rings',2001,8,'../../public/lotr.jpg','O fratie porneste la drum pentru a distruge Inelul Stapanului.','https://www.youtube.com/embed/V75dMMIW2B4',0);
INSERT INTO movies VALUES(19,'Raiders of the Lost Ark',1981,9,'../../public/indiana.jpg','Arheologul Indiana Jones cauta Chivotul Legamantului.','https://www.youtube.com/embed/XkkzKjKCLKY',0);
INSERT INTO movies VALUES(20,'Our Planet',2019,10,'../../public/ourplanet.jpg','David Attenborough exploreaza frumusetea si fragilitatea planetei noastre.','https://www.youtube.com/embed/ICwZp-4f3H0',0);
INSERT INTO movies VALUES(21,'The Good the Bad and the Ugly',1966,11,'../../public/goodbadugly.jpg','Trei pistolari rivali cauta o comoara ascunsa din timpul Razboiului Civil.','https://www.youtube.com/embed/WCN5JJY_wiA',0);
INSERT INTO movies VALUES(22,'The Greatest Showman',2017,12,'../../public/showman.jpg','Povestea lui P.T. Barnum si crearea celui mai mare spectacol de pe pamant.','https://www.youtube.com/embed/jr9QtXwC9vc',0);
INSERT INTO movies VALUES(23,'Knives Out',2019,13,'../../public/knivesout.jpg','Un detectiv excentric investigheaza moartea unui scriitor celebru.','https://www.youtube.com/embed/qGqiHJTsRkQ',0);
INSERT INTO movies VALUES(24,'Oppenheimer',2023,14,'../../public/oppenheimer.jpg','Povestea omului care a creat bomba atomica si dilema morala care l-a urmarit.','https://www.youtube.com/embed/uYPbbksJxIg',0);
COMMIT;

-- Avansăm secvența la 25 ca să nu se ciocnească cu INSERT-urile fixe
BEGIN
    WHILE SEQ_MOVIES.NEXTVAL < 25 LOOP NULL; END LOOP;
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- MOVIE_DIRECTORS (consistent cu cele 24 filme)
INSERT INTO movie_directors VALUES (1, 3);  -- Godfather - Coppola
INSERT INTO movie_directors VALUES (2, 8);  -- Whiplash - Chazelle
INSERT INTO movie_directors VALUES (3, 2);  -- Pianist - Scorsese
INSERT INTO movie_directors VALUES (4, 1);  -- Inception - Nolan
INSERT INTO movie_directors VALUES (5, 7);  -- Mad Max - Phillips
INSERT INTO movie_directors VALUES (6, 7);  -- Hangover - Phillips
INSERT INTO movie_directors VALUES (7, 10); -- Barbie - Gerwig
INSERT INTO movie_directors VALUES (8, 7);  -- Joker - Phillips
INSERT INTO movie_directors VALUES (9, 2);  -- Shutter Island - Scorsese
INSERT INTO movie_directors VALUES (10, 9); -- Se7en - Fincher
INSERT INTO movie_directors VALUES (11, 1); -- Interstellar - Nolan
INSERT INTO movie_directors VALUES (12,16); -- Blade Runner - Villeneuve
INSERT INTO movie_directors VALUES (13, 5); -- Your Name - Shinkai
INSERT INTO movie_directors VALUES (14, 6); -- Spirited Away - Miyazaki
INSERT INTO movie_directors VALUES (15,15); -- Silent Voice - Joon-ho
INSERT INTO movie_directors VALUES (16, 4); -- Titanic - Cameron
INSERT INTO movie_directors VALUES (17, 8); -- La La Land - Chazelle
INSERT INTO movie_directors VALUES (18,11); -- LOTR - Jackson
INSERT INTO movie_directors VALUES (19,17); -- Indiana Jones - Spielberg
INSERT INTO movie_directors VALUES (20,18); -- Our Planet - Fothergill
INSERT INTO movie_directors VALUES (21,19); -- Good Bad Ugly - Leone
INSERT INTO movie_directors VALUES (22,20); -- Greatest Showman - Gracey
INSERT INTO movie_directors VALUES (23, 1); -- Knives Out - Nolan
INSERT INTO movie_directors VALUES (24, 1); -- Oppenheimer - Nolan
COMMIT;

-- MOVIE_ACTORS (consistent cu cele 24 filme)
INSERT INTO movie_actors VALUES (1, 7, 'Vito Corleone');
INSERT INTO movie_actors VALUES (2, 10,'Andrew Neiman');
INSERT INTO movie_actors VALUES (3, 14,'Szpilman');
INSERT INTO movie_actors VALUES (4, 1, 'Cobb');
INSERT INTO movie_actors VALUES (4, 6, 'Ariadne');
INSERT INTO movie_actors VALUES (5, 2, 'Max');
INSERT INTO movie_actors VALUES (6, 12,'Phil');
INSERT INTO movie_actors VALUES (7, 3, 'Barbie');
INSERT INTO movie_actors VALUES (7, 9, 'Ken');
INSERT INTO movie_actors VALUES (8, 5, 'Arthur Fleck');
INSERT INTO movie_actors VALUES (9, 1, 'Teddy Daniels');
INSERT INTO movie_actors VALUES (10, 2,'David Mills');
INSERT INTO movie_actors VALUES (10,11,'Somerset');
INSERT INTO movie_actors VALUES (11, 4,'Cooper');
INSERT INTO movie_actors VALUES (11, 6,'Brand');
INSERT INTO movie_actors VALUES (12, 9,'K');
INSERT INTO movie_actors VALUES (13,15,'Mitsuha');
INSERT INTO movie_actors VALUES (14,15,'Chihiro');
INSERT INTO movie_actors VALUES (15,15,'Shoya');
INSERT INTO movie_actors VALUES (16, 1,'Jack Dawson');
INSERT INTO movie_actors VALUES (17, 8,'Mia');
INSERT INTO movie_actors VALUES (17, 9,'Sebastian');
INSERT INTO movie_actors VALUES (18,16,'Frodo');
INSERT INTO movie_actors VALUES (19,17,'Indiana Jones');
INSERT INTO movie_actors VALUES (20,18,'Narrator');
INSERT INTO movie_actors VALUES (21,19,'Blondie');
INSERT INTO movie_actors VALUES (22,20,'Barnum');
INSERT INTO movie_actors VALUES (23,11,'Benoit Blanc');
INSERT INTO movie_actors VALUES (24,10,'Oppenheimer');
COMMIT;

-- FORMATE
DECLARE
    v_id NUMBER;
BEGIN
    FOR r IN (SELECT id FROM movies ORDER BY id) LOOP
        v_id := SEQ_MOVIE_FORMATS.NEXTVAL;
        INSERT INTO movie_formats VALUES(v_id, r.id, 'Streaming Online', '4K Ultra HD',
            'https://stream.cinematheque.ro/watch/' || r.id || '/4k');
        v_id := SEQ_MOVIE_FORMATS.NEXTVAL;
        INSERT INTO movie_formats VALUES(v_id, r.id, 'Download', '1080p Full HD',
            'https://dl.cinematheque.ro/movie/' || r.id || '/1080p.mp4');
    END LOOP;
END;
/
COMMIT;

-- USERI (21 — fără STANDARD_HASH)
DECLARE
    v_id NUMBER;
BEGIN
    FOR i IN 1..20 LOOP
        v_id := SEQ_USERS.NEXTVAL;
        INSERT INTO users(id, full_name, email, password_hash)
        VALUES(v_id, 'Utilizator '||i, 'user'||i||'@cinematheque.ro',
               RAWTOHEX(UTL_RAW.CAST_TO_RAW('parola'||i)));
    END LOOP;
    v_id := SEQ_USERS.NEXTVAL;
    INSERT INTO users(id, full_name, email, password_hash)
    VALUES(v_id, 'Administrator', 'admin@cinematheque.ro',
           RAWTOHEX(UTL_RAW.CAST_TO_RAW('admin123')));
    COMMIT;
END;
/

-- WATCH_HISTORY (ROWNUM în loc de FETCH FIRST)
DECLARE
    v_format_id NUMBER;
    v_days_ago  NUMBER;
    v_limit     NUMBER;
BEGIN
    FOR u IN 1..21 LOOP
        v_limit := 3 + MOD(u, 6);
        FOR f IN (
            SELECT id, id_category FROM (
                SELECT id, id_category FROM movies ORDER BY DBMS_RANDOM.VALUE
            ) WHERE ROWNUM <= v_limit
        ) LOOP
            BEGIN
                SELECT id INTO v_format_id FROM movie_formats
                WHERE id_movie = f.id AND ROWNUM = 1;
                v_days_ago := TRUNC(DBMS_RANDOM.VALUE(1, 120));
                INSERT INTO watch_history(id_user, id_movie, id_format, watch_date)
                VALUES(u, f.id, v_format_id,
                       CURRENT_TIMESTAMP - INTERVAL '1' DAY * v_days_ago);
            EXCEPTION WHEN DUP_VAL_ON_INDEX THEN NULL;
            END;
        END LOOP;
    END LOOP;
    COMMIT;
END;
/

-- RATINGS
DECLARE
    v_rating NUMBER;
    v_rid    NUMBER;
BEGIN
    FOR u IN 1..21 LOOP
        FOR f IN (SELECT DISTINCT id_movie FROM watch_history WHERE id_user = u) LOOP
            v_rating := ROUND(DBMS_RANDOM.VALUE(6, 10) * 2) / 2; -- multiplu de 0.5 intre 6-10
            v_rid := SEQ_RATINGS.NEXTVAL;
            BEGIN
                INSERT INTO movie_ratings(id, id_user, id_movie, rating)
                VALUES(v_rid, u, f.id_movie, v_rating);
            EXCEPTION WHEN DUP_VAL_ON_INDEX THEN NULL;
            WHEN OTHERS THEN NULL;
            END;
        END LOOP;
    END LOOP;
    COMMIT;
END;
/

-- COMENTARII
DECLARE
    TYPE t_comments IS TABLE OF VARCHAR2(200) INDEX BY PLS_INTEGER;
    v_comments t_comments;
    v_cid      NUMBER;
    idx        NUMBER := 1;
BEGIN
    v_comments(1)  := 'Un film de referinta, il recomand cu caldura!';
    v_comments(2)  := 'Regie impecabila si actorie de exceptie.';
    v_comments(3)  := 'M-a tinut cu sufletul la gura de la inceput pana la final.';
    v_comments(4)  := 'Nu m-am asteptat la un final atat de puternic.';
    v_comments(5)  := 'Cinematografie absolut magnifica.';
    v_comments(6)  := 'Coloana sonora mi s-a infipt in minte pentru zile intregi.';
    v_comments(7)  := 'Un clasic care rezista testului timpului.';
    v_comments(8)  := 'Poate cel mai bun film din gen pe care l-am vazut.';
    v_comments(9)  := 'Scenariu complex, dar merita fiecare secunda de atentie.';
    v_comments(10) := 'Actoria principala este pur si simplu uluitoare.';
    v_comments(11) := 'Finalul m-a lasat mut de uimire.';
    v_comments(12) := 'Vizual spectaculos, o poveste captivanta.';
    v_comments(13) := 'L-am revazut de trei ori si tot descopar detalii noi.';
    v_comments(14) := 'Emotional pana la lacrimi. O capodopera.';
    v_comments(15) := 'Ritmul e perfect, nu exista nicio scena de umplutura.';

    FOR wh IN (
        SELECT id_user, id_movie FROM watch_history
        WHERE id_user <= 5 AND ROWNUM <= 15
    ) LOOP
        v_cid := SEQ_MOVIE_COMMENTS.NEXTVAL;
        INSERT INTO movie_comments(id, id_user, id_movie, comment_text)
        VALUES(v_cid, wh.id_user, wh.id_movie, v_comments(idx));
        idx := MOD(idx, 15) + 1;
    END LOOP;
    COMMIT;
END;
/

-- WATCHLIST
DECLARE
BEGIN
    FOR u IN 1..10 LOOP
        FOR f IN (
            SELECT id FROM (
                SELECT id FROM movies
                WHERE id NOT IN (SELECT id_movie FROM watch_history WHERE id_user = u)
                ORDER BY DBMS_RANDOM.VALUE
            ) WHERE ROWNUM <= 3
        ) LOOP
            BEGIN
                INSERT INTO watchlist(id_user, id_movie) VALUES(u, f.id);
            EXCEPTION WHEN DUP_VAL_ON_INDEX THEN NULL;
            END;
        END LOOP;
    END LOOP;
    COMMIT;
END;
/

-- ==========================================================
-- 9. VERIFICARE FINALĂ
-- ==========================================================
SELECT 'categories'      t, COUNT(*) n FROM categories      UNION ALL
SELECT 'users',            COUNT(*) FROM users               UNION ALL
SELECT 'directors',        COUNT(*) FROM directors           UNION ALL
SELECT 'actors',           COUNT(*) FROM actors              UNION ALL
SELECT 'movies',           COUNT(*) FROM movies              UNION ALL
SELECT 'movie_formats',    COUNT(*) FROM movie_formats       UNION ALL
SELECT 'movie_directors',  COUNT(*) FROM movie_directors     UNION ALL
SELECT 'movie_actors',     COUNT(*) FROM movie_actors        UNION ALL
SELECT 'watch_history',    COUNT(*) FROM watch_history       UNION ALL
SELECT 'movie_ratings',    COUNT(*) FROM movie_ratings       UNION ALL
SELECT 'movie_comments',   COUNT(*) FROM movie_comments      UNION ALL
SELECT 'watchlist',        COUNT(*) FROM watchlist;
