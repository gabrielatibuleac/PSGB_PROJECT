SET SERVEROUTPUT ON;
ALTER SESSION SET PLSCOPE_SETTINGS = 'IDENTIFIERS:NONE';

-- 1. CURĂȚENIE TOTALĂ
BEGIN
    EXECUTE IMMEDIATE 'DELETE FROM watch_history';
    EXECUTE IMMEDIATE 'DELETE FROM movie_comments';
    EXECUTE IMMEDIATE 'DELETE FROM reviews';
    EXECUTE IMMEDIATE 'DELETE FROM watchlist';
    EXECUTE IMMEDIATE 'DELETE FROM movie_actors';
    EXECUTE IMMEDIATE 'DELETE FROM movie_directors';
    EXECUTE IMMEDIATE 'DELETE FROM movie_formats';
    EXECUTE IMMEDIATE 'DELETE FROM movies';
    EXECUTE IMMEDIATE 'DELETE FROM actors';
    EXECUTE IMMEDIATE 'DELETE FROM directors';
    EXECUTE IMMEDIATE 'DELETE FROM users';
    EXECUTE IMMEDIATE 'DELETE FROM categories';
    COMMIT;
END;
/

-- 2. CATEGORII (7)
INSERT INTO categories (id, name) VALUES (1, 'Drama');
INSERT INTO categories (id, name) VALUES (2, 'Action');
INSERT INTO categories (id, name) VALUES (3, 'Comedy');
INSERT INTO categories (id, name) VALUES (4, 'Thriller');
INSERT INTO categories (id, name) VALUES (5, 'Sci-Fi');
INSERT INTO categories (id, name) VALUES (6, 'Anime');
INSERT INTO categories (id, name) VALUES (7, 'Romance');
INSERT INTO categories (id, name) VALUES (8, 'Fantasy');
INSERT INTO categories (id, name) VALUES (9, 'Adventure');
INSERT INTO categories (id, name) VALUES (10, 'Documentary');
INSERT INTO categories (id, name) VALUES (11, 'Western');
INSERT INTO categories (id, name) VALUES (12, 'Musical');
INSERT INTO categories (id, name) VALUES (13, 'Mystery');
INSERT INTO categories (id, name) VALUES (14, 'Historical');
INSERT INTO categories (id, name) VALUES (15, 'Noir');

-- 3. REGIZORI (15 - conform cerinței)
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (1, 'Christopher', 'Nolan', '../../public/nolan.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (2, 'Martin', 'Scorsese', '../../public/scorsese.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (3, 'Francis Ford', 'Coppola', '../../public/cappola.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (4, 'James', 'Cameron', '../../public/cameron.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (5, 'Makoto', 'Shinkai', '../../public/shinkai.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (6, 'Hayao', 'Miyazaki', '../../public/miyazaki.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (7, 'Todd', 'Phillips', '../../public/phillips.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (8, 'Damien', 'Chazelle', '../../public/damien.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (9, 'David', 'Fincher', '../../public/nick.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (10, 'Greta', 'Gerwig', '../../public/gerwig.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (11, 'Peter', 'Jackson', '../../public/peter.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (12, 'Quentin', 'Tarantino', '../../public/quetin.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (13, 'Frank', 'Darabont', '../../public/frank.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (14, 'Ridley', 'Scott', '../../public/scott.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (15, 'Bong', 'Joon-ho', '../../public/bong.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (23, 'Steven', 'Spielberg', '../../public/spielberg.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (24, 'Alastair', 'Fothergill', '../../public/fothergill.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (25, 'Sergio', 'Leone', '../../public/leone.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (26, 'Michael', 'Gracey', '../../public/gracey.jpg');
INSERT INTO directors (id, first_name, last_name, photo_url) VALUES (27, 'Rian', 'Johnson', '../../public/johnson.jpg');

-- 4. ACTORI (15 - conform cerinței)
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (1, 'Leonardo', 'DiCaprio', '../../public/dicaprio.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (2, 'Brad', 'Pitt', '../../public/pitt.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (3, 'Margot', 'Robbie', '../../public/robbie.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (4, 'Matthew', 'McConaughey', '../../public/matthew.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (5, 'Joaquin', 'Phoenix', '../../public/phoenix.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (6, 'Anne', 'Hathaway', '../../public/anne.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (7, 'Marlon', 'Brando', '../../public/brando.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (8, 'Emma', 'Stone', '../../public/stone.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (9, 'Ryan', 'Gosling', '../../public/reynolds.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (10, 'Christian', 'Bale', '../../public/bale.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (11, 'Morgan', 'Freeman', '../../public/morgan.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (12, 'Tom', 'Hanks', '../../public/tom.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (13, 'Meryl', 'Streep', '../../public/streep.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (14, 'Robert', 'De Niro', '../../public/robert.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (15, 'Scarlett', 'Johansson', '../../public/scarllet.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (23, 'Elijah', 'Wood', '../../public/wood.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (24, 'Harrison', 'Ford', '../../public/ford.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (25, 'David', 'Attenborough', '../../public/david.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (26, 'Clint', 'Eastwood', '../../public/clint.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (27, 'Hugh', 'Jackman', '../../public/hugh.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (28, 'Daniel', 'Craig', '../../public/craig.jpg');
INSERT INTO actors (id, first_name, last_name, photo_url) VALUES (29, 'Cillian', 'Murphy', '../../public/murphy.jpg');

-- 5. FILME (18 filme reale)
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (1, 'The Godfather', 1972, 1, '../../public/poster1.jpg', 'Povestea familiei Corleone.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (2, 'Whiplash', 2014, 1, '../../public/poster5.jpg', 'Obsesia unui toboșar pentru perfecțiune.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (3, 'The Pianist', 2002, 1, '../../public/poster4.jpg', 'Supraviețuirea unui muzician în Varșovia.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (4, 'Inception', 2010, 2, '../../public/poster0.jpg', 'Hoți de vise într-o lume corporatistă.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (5, 'Mad Max: Fury Road', 2015, 2, '../../public/poster_madmax.jpg', 'Urmăriri spectaculoase în deșert.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (6, 'Deadpool', 2016, 2, '../../public/deadpool.jpg', 'Un erou atipic și vulgar.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (7, 'The Hangover', 2009, 3, '../../public/hangover.jpg', 'Trei prieteni pierduți în Las Vegas.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (8, 'Barbie', 2023, 3, '../../public/barbie.jpg', 'O criză existențială în lumea roz.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (9, 'Joker', 2019, 4, '../../public/poster_joker.jpg', 'Originea infamului antagonist din Gotham.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (10, 'Shutter Island', 2010, 4, '../../public/poster_shutter.jpg', 'Investigație pe o insulă misterioasă.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (11, 'Se7en', 1995, 4, '../../public/se7en.jpg', 'Doi detectivi vânează un criminal în serie.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (12, 'Interstellar', 2014, 5, '../../public/poster0.jpg', 'Salvarea omenirii prin spațiu și timp.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (13, 'Your Name', 2016, 6, '../../public/yourname.jpg', 'Schimb de corpuri între doi adolescenți.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (14, 'Spirited Away', 2001, 6, '../../public/poster3.jpg', 'O fetiță rătăcită în lumea spiritelor.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (15, 'Silent Voice', 2016, 6, '../../public/silentvoice.jpg', 'O poveste emoționantă despre iertare.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (16, 'Titanic', 1997, 7, '../../public/titanic.jpg', 'Iubire pe nava care nu se poate scufunda.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (17, 'The Notebook', 2004, 7, '../../public/notebook.jpg', 'O dragoste eternă scrisă într-un jurnal.');
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (18, 'La La Land', 2016, 7, '../../public/lalaland.jpg', 'Doi artiști se îndrăgostesc în Los Angeles.');
-- 8. Fantasy
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (26, 'The Lord of the Rings: The Fellowship of the Ring', 2001, 8, '../../public/lotr.jpg', 'O frăție pornește la drum pentru a distruge un inel malefic.');
-- 9. Adventure
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (27, 'Raiders of the Lost Ark', 1981, 9, '../../public/indiana.jpg', 'Arheologul Indiana Jones caută Chivotul Legământului.');
-- 10. Documentar
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (28, 'Our Planet', 2019, 10, '../../public/ourplanet.jpg', 'O explorare uimitoare a frumuseții și fragilității planetei noastre.');
-- 11. Western
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (29, 'The Good, the Bad and the Ugly', 1966, 11, '../../public/goodbadugly.jpg', 'Trei pistolari caută o comoară ascunsă în timpul Războiului Civil.');
-- 12. Musical
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (30, 'The Greatest Showman', 2017, 12, '../../public/showman.jpg', 'Povestea vizionarului P.T. Barnum și a spectacolului său magic.');
-- 13. Mister
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (31, 'Knives Out', 2019, 13, '../../public/knivesout.jpg', 'Un detectiv investighează moartea unui patriarh excentric.');
-- 14. Istoric / Biografic
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (32, 'Oppenheimer', 2023, 14, '../../public/oppenheimer.jpg', 'Povestea fizicianului J. Robert Oppenheimer și crearea bombei atomice.');
-- 15. Noir / Neo-Noir
INSERT INTO movies (id, title, release_year, id_category, poster_url, description) VALUES (33, 'Blade Runner 2049', 2017, 15, '../../public/bladerunner.jpg', 'Un nou vânător de replicanți descoperă un secret îngropat demult.');
-- 6. LEGARE REGIZORI (Pentru TOATE cele 18 filme)
INSERT INTO movie_directors (id_movie, id_director) VALUES (1, 3); -- Godfather
INSERT INTO movie_directors (id_movie, id_director) VALUES (2, 8); -- Whiplash
INSERT INTO movie_directors (id_movie, id_director) VALUES (3, 2); -- Pianist
INSERT INTO movie_directors (id_movie, id_director) VALUES (4, 1); -- Inception
INSERT INTO movie_directors (id_movie, id_director) VALUES (5, 7); -- Mad Max
INSERT INTO movie_directors (id_movie, id_director) VALUES (6, 12); -- Deadpool
INSERT INTO movie_directors (id_movie, id_director) VALUES (7, 7); -- Hangover
INSERT INTO movie_directors (id_movie, id_director) VALUES (8, 10); -- Barbie
INSERT INTO movie_directors (id_movie, id_director) VALUES (9, 7); -- Joker
INSERT INTO movie_directors (id_movie, id_director) VALUES (10, 2); -- Shutter Island
INSERT INTO movie_directors (id_movie, id_director) VALUES (11, 9); -- Se7en
INSERT INTO movie_directors (id_movie, id_director) VALUES (12, 1); -- Interstellar
INSERT INTO movie_directors (id_movie, id_director) VALUES (13, 5); -- Your Name
INSERT INTO movie_directors (id_movie, id_director) VALUES (14, 6); -- Spirited Away
INSERT INTO movie_directors (id_movie, id_director) VALUES (15, 15); -- Silent Voice
INSERT INTO movie_directors (id_movie, id_director) VALUES (16, 4); -- Titanic
INSERT INTO movie_directors (id_movie, id_director) VALUES (17, 13); -- Notebook
INSERT INTO movie_directors (id_movie, id_director) VALUES (18, 8); -- La La Land
INSERT INTO movie_directors (id_movie, id_director) VALUES (26, 11); -- LotR - Peter Jackson (ID 11 din scriptul tău inițial)
INSERT INTO movie_directors (id_movie, id_director) VALUES (27, 23); -- Indiana Jones - Steven Spielberg
INSERT INTO movie_directors (id_movie, id_director) VALUES (28, 24); -- Our Planet - Alastair Fothergill
INSERT INTO movie_directors (id_movie, id_director) VALUES (29, 25); -- The Good, the Bad... - Sergio Leone
INSERT INTO movie_directors (id_movie, id_director) VALUES (30, 26); -- The Greatest Showman - Michael Gracey
INSERT INTO movie_directors (id_movie, id_director) VALUES (31, 27); -- Knives Out - Rian Johnson
INSERT INTO movie_directors (id_movie, id_director) VALUES (32, 1);  -- Oppenheimer - Christopher Nolan (ID 1 din scriptul inițial)
INSERT INTO movie_directors (id_movie, id_director) VALUES (33, 16); -- Blade Runner 2049 - Denis Villeneuve (ID 16 adăugat la pasul anterior)

-- 7. LEGARE ACTORI (Pentru TOATE cele 18 filme)
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (1, 7, 'Vito Corleone');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (2, 10, 'Andrew Neiman');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (3, 14, 'Szpilman');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (4, 1, 'Cobb');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (5, 2, 'Max');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (6, 9, 'Wade Wilson');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (7, 12, 'Phil');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (8, 3, 'Barbie');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (9, 5, 'Arthur Fleck');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (10, 1, 'Teddy Daniels');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (11, 2, 'David Mills');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (12, 4, 'Cooper');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (13, 15, 'Mitsuha (Voice)');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (14, 15, 'Chihiro (Voice)');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (15, 15, 'Shoko (Voice)');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (16, 1, 'Jack Dawson');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (17, 9, 'Noah');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (18, 8, 'Mia');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (26, 23, 'Frodo Baggins');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (27, 24, 'Indiana Jones');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (28, 25, 'Narrator (Voice)');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (29, 26, 'Blondie');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (30, 27, 'P.T. Barnum');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (31, 28, 'Benoit Blanc');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (32, 29, 'J. Robert Oppenheimer');
INSERT INTO movie_actors (id_movie, id_actor, role_name) VALUES (33, 9, 'K'); -- Ryan Gosling (ID 9 din scriptul inițial)
-- 8. FORMATE (Minim 15)
BEGIN
    FOR i IN 1..18 LOOP
        INSERT INTO movie_formats (id, id_movie, format_name, quality_label) VALUES (i, i, 'Streaming', '4K');
    END LOOP;
END;
/

-- 9. UTILIZATORI (20)
BEGIN
    FOR i IN 1..20 LOOP
        INSERT INTO users (id, full_name, email, password_hash) 
        VALUES (i, 'User_'||i, 'user'||i||'@test.com', 'pass123');
    END LOOP;
END;
/

-- 10. COMENTARII (Minim 15)
BEGIN
    FOR i IN 1..18 LOOP
        INSERT INTO movie_comments (id_user, id_movie, comment_text) VALUES (1, i, 'Un film excelent, recomand!');
    END LOOP;
END;
/

-- 11. WATCHLIST (Minim 15)
BEGIN
    FOR i IN 1..18 LOOP
        INSERT INTO watchlist (id_user, id_movie) VALUES (2, i);
    END LOOP;
END;
/

BEGIN
    FOR i IN 1..33 LOOP
        INSERT INTO movie_formats (id, id_movie, format_name, quality_label) VALUES (i, i, 'Streaming', '4K');
    END LOOP;
END;
/

BEGIN
    FOR i IN 1..33 LOOP
        INSERT INTO movie_comments (id_user, id_movie, comment_text) VALUES (1, i, 'Un film excelent, reprezentativ pentru genul lui!');
    END LOOP;
END;
/

BEGIN
    FOR i IN 1..33 LOOP
        INSERT INTO watchlist (id_user, id_movie) VALUES (2, i);
    END LOOP;
END;
/

COMMIT;
