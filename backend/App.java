import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class App {

    private static String clean(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", " ").replace("\r", "").trim();
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        String dbUrl = "jdbc:oracle:thin:@localhost:1521:xe";
        String dbUser = "student"; 
        String dbPass = "STUDENT";

        // --- 1. LOGIN ---
        server.createContext("/login", (exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String q = exchange.getRequestURI().getQuery();
            String response = "0"; 
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                String email = "", pass = "";
                for (String pair : q.split("&")) {
                    String[] kv = pair.split("=");
                    if (kv.length > 1) {
                        if (kv[0].equals("email")) email = URLDecoder.decode(kv[1], "UTF-8");
                        if (kv[0].equals("pass")) pass = URLDecoder.decode(kv[1], "UTF-8");
                    }
                }
                PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE LOWER(email) = LOWER(?) AND PASSWORD_HASH = ?");
                ps.setString(1, email);
                ps.setString(2, pass);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) response = String.valueOf(rs.getInt(1));
            } catch (Exception e) { e.printStackTrace(); }
            byte[] bs = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bs.length);
            exchange.getResponseBody().write(bs);
            exchange.getResponseBody().close();
        });

        // --- 2. REGISTER ---
        server.createContext("/register", (exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String q = exchange.getRequestURI().getQuery();
            String response = "0";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                String name = "", email = "", pass = "";
                for (String pair : q.split("&")) {
                    String[] kv = pair.split("=");
                    if (kv.length > 1) {
                        if (kv[0].equals("name")) name = URLDecoder.decode(kv[1], "UTF-8");
                        if (kv[0].equals("email")) email = URLDecoder.decode(kv[1], "UTF-8");
                        if (kv[0].equals("pass")) pass = URLDecoder.decode(kv[1], "UTF-8");
                    }
                }
                String sql = "INSERT INTO users (full_name, email, PASSWORD_HASH) VALUES (?, ?, ?)";
                String[] generatedId = {"ID"};
                PreparedStatement ps = conn.prepareStatement(sql, generatedId);
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, pass);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) response = String.valueOf(rs.getInt(1));
            } catch (Exception e) { e.printStackTrace(); }
            byte[] bs = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bs.length);
            exchange.getResponseBody().write(bs);
            exchange.getResponseBody().close();
        });

        // --- 3. PROFIL (REPARAT) ---
        server.createContext("/profil", (exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            String q = exchange.getRequestURI().getQuery();
            String id = (q != null && q.contains("id=")) ? q.split("id=")[1].split("&")[0] : "0";
            String response = "{\"nume\":\"Eroare\",\"email\":\"-\"}";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                PreparedStatement ps = conn.prepareStatement("SELECT full_name, email FROM users WHERE id = ?");
                ps.setInt(1, Integer.parseInt(id));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    response = "{\"nume\":\"" + clean(rs.getString(1)) + "\",\"email\":\"" + clean(rs.getString(2)) + "\"}";
                }
            } catch (Exception e) { e.printStackTrace(); }
            byte[] bs = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bs.length);
            exchange.getResponseBody().write(bs);
            exchange.getResponseBody().close();
        });

        // --- 4. LISTĂ FILME ---
        server.createContext("/filme", (exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            String q = exchange.getRequestURI().getQuery();
            String sql = "SELECT id, title, release_year, poster_url FROM movies WHERE 1=1";
            if (q != null) {
                if (q.contains("cat=")) sql += " AND id_category = " + q.split("cat=")[1].split("&")[0];
                if (q.contains("search=")) sql += " AND LOWER(title) LIKE LOWER('%" + q.split("search=")[1].split("&")[0] + "%')";
            }
            StringBuilder sb = new StringBuilder("[");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                ResultSet rs = conn.createStatement().executeQuery(sql);
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{\"id\":").append(rs.getInt(1))
                      .append(",\"titlu\":\"").append(clean(rs.getString(2)))
                      .append("\",\"an\":").append(rs.getInt(3))
                      .append(",\"imagine\":\"").append(clean(rs.getString(4))).append("\"}");
                    first = false;
                }
            } catch (Exception e) { e.printStackTrace(); }
            sb.append("]");
            byte[] res = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, res.length);
            exchange.getResponseBody().write(res);
            exchange.getResponseBody().close();
        });

        // --- 5. DETALII FILM ---
        // --- DETALII FILM MODIFICAT ---
// --- 2. DETALII FILM (MODIFICAT PENTRU TRAILER) ---
        server.createContext("/movie-details", (exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            String q = exchange.getRequestURI().getQuery();
            String mid = (q != null && q.contains("id=")) ? q.split("id=")[1].split("&")[0] : "3";
            StringBuilder json = new StringBuilder("{");

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                // PASUL 1: Am adăugat "trailer_url" în interogarea SQL (este coloana numărul 5)
                // În App.java, la contextul /movie-details:
PreparedStatement ps = conn.prepareStatement("SELECT title, description, release_year, poster_url, trailer_url FROM movies WHERE id = ?");
ps.setInt(1, Integer.parseInt(mid));
ResultSet rs = ps.executeQuery();

if (rs.next()) {
    json.append("\"titlu\":\"").append(clean(rs.getString("title"))).append("\",")
        .append("\"descriere\":\"").append(clean(rs.getString("description"))).append("\",")
        .append("\"an\":\"").append(rs.getString("release_year")).append("\",")
        .append("\"imagine\":\"").append(clean(rs.getString("poster_url"))).append("\",")
        .append("\"trailer\":\"").append(clean(rs.getString("trailer_url"))).append("\",");
}
                // --- Partea de Directori ---
                json.append("\"directori\":[");
                ResultSet rd = conn.createStatement().executeQuery("SELECT d.first_name||' '||d.last_name, d.photo_url FROM directors d JOIN movie_directors md ON d.id=md.id_director WHERE md.id_movie="+mid);
                if (rd.next()) json.append("{\"nume\":\"").append(clean(rd.getString(1))).append("\",\"poza\":\"").append(clean(rd.getString(2))).append("\"}");
                
                // --- Partea de Actori ---
                json.append("],\"actori\":[");
                ResultSet ra = conn.createStatement().executeQuery("SELECT a.first_name||' '||a.last_name, ma.role_name, a.photo_url FROM actors a JOIN movie_actors ma ON a.id=ma.id_actor WHERE ma.id_movie="+mid);
                boolean first = true;
                while (ra.next()) {
                    if (!first) json.append(",");
                    json.append("{\"nume\":\"").append(clean(ra.getString(1))).append("\",\"rol\":\"").append(clean(ra.getString(2))).append("\",\"poza\":\"").append(clean(ra.getString(3))).append("\"}");
                    first = false;
                }
                json.append("]}");

            } catch (Exception e) { 
                e.printStackTrace(); 
                json = new StringBuilder("{\"eroare\":\"DB\"}"); 
            }

            byte[] res = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, res.length);
            exchange.getResponseBody().write(res);
            exchange.getResponseBody().close();
        });
        server.createContext("/recomandare", (exchange) -> {
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    String q = exchange.getRequestURI().getQuery();
    String uId = q.split("id=")[1];
    String movieRec = "";
    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
        // APEL PROCEDURĂ PL/SQL
        CallableStatement cs = conn.prepareCall("{call get_recommendation(?, ?)}");
        cs.setInt(1, Integer.parseInt(uId));
        cs.registerOutParameter(2, java.sql.Types.VARCHAR);
        cs.execute();
        movieRec = cs.getString(2);
    } catch (Exception e) { movieRec = "Eroare recomandare"; }
    
    String response = "{\"recomandare\":\"" + movieRec + "\"}";
    exchange.sendResponseHeaders(200, response.length());
    exchange.getResponseBody().write(response.getBytes());
    exchange.getResponseBody().close();
});

// --- VERIFICARE DACĂ ESTE ÎN WATCHLIST ---
server.createContext("/check-watchlist", (exchange) -> {
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    String q = exchange.getRequestURI().getQuery();
    String response = "false";
    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
        int uId = Integer.parseInt(q.split("user=")[1].split("&")[0]);
        int mId = Integer.parseInt(q.split("movie=")[1].split("&")[0]);
        PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM watchlist WHERE id_user=? AND id_movie=?");
        ps.setInt(1, uId);
        ps.setInt(2, mId);
        ResultSet rs = ps.executeQuery();
        if (rs.next() && rs.getInt(1) > 0) response = "true";
    } catch (Exception e) { e.printStackTrace(); }
    exchange.sendResponseHeaders(200, response.length());
    exchange.getResponseBody().write(response.getBytes());
    exchange.getResponseBody().close();
});
        // --- 6. COMENTARII ---
        server.createContext("/get-comments", (exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            String q = exchange.getRequestURI().getQuery();
            String mid = (q != null && q.contains("id=")) ? q.split("id=")[1].split("&")[0] : "0";
            StringBuilder sb = new StringBuilder("[");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                String sql = "SELECT u.full_name, c.comment_text FROM movie_comments c JOIN users u ON c.id_user = u.id WHERE c.id_movie = ? ORDER BY c.id DESC";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, Integer.parseInt(mid));
                ResultSet rs = ps.executeQuery();
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{\"autor\":\"").append(clean(rs.getString(1))).append("\",\"text\":\"").append(clean(rs.getString(2))).append("\"}");
                    first = false;
                }
            } catch (Exception e) { e.printStackTrace(); }
            sb.append("]");
            byte[] res = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, res.length);
            exchange.getResponseBody().write(res);
            exchange.getResponseBody().close();
        });
        // --- FETCH FORMATE FILM ---
server.createContext("/get-formats", (exchange) -> {
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    String q = exchange.getRequestURI().getQuery();
    String mid = q.split("id=")[1].split("&")[0];
    StringBuilder sb = new StringBuilder("[");
    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
        ResultSet rs = conn.createStatement().executeQuery("SELECT id, format_name, quality_label, source_link FROM movie_formats WHERE id_movie = " + mid);
        boolean first = true;
        while(rs.next()){
            if(!first) sb.append(",");
            sb.append("{\"id\":").append(rs.getInt(1)).append(",\"nume\":\"").append(clean(rs.getString(2)))
              .append("\",\"calitate\":\"").append(clean(rs.getString(3))).append("\",\"link\":\"").append(clean(rs.getString(4))).append("\"}");
            first = false;
        }
    } catch (Exception e) { e.printStackTrace(); }
    sb.append("]");
    byte[] res = sb.toString().getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(200, res.length);
    exchange.getResponseBody().write(res);
    exchange.getResponseBody().close();
});

// --- LOG WATCH (Adăugare în Istoric) ---
server.createContext("/log-watch", (exchange) -> {
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    String q = exchange.getRequestURI().getQuery();
    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
        int uId = Integer.parseInt(q.split("user=")[1].split("&")[0]);
        int mId = Integer.parseInt(q.split("movie=")[1].split("&")[0]);
        int fId = Integer.parseInt(q.split("format=")[1].split("&")[0]);
        PreparedStatement ps = conn.prepareStatement("INSERT INTO watch_history (id_user, id_movie, id_format) VALUES (?, ?, ?)");
        ps.setInt(1, uId);
        ps.setInt(2, mId);
        ps.setInt(3, fId);
        ps.executeUpdate();
    } catch (Exception e) { e.printStackTrace(); }
    exchange.sendResponseHeaders(200, 2);
    exchange.getResponseBody().write("OK".getBytes());
    exchange.getResponseBody().close();
});

// --- FETCH ISTORIC (Pentru Pagina Contului) ---
server.createContext("/get-history", (exchange) -> {
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    String q = exchange.getRequestURI().getQuery();
    String uId = q.split("user=")[1].split("&")[0];
    StringBuilder sb = new StringBuilder("[");
    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
        String sql = "SELECT m.title, f.format_name, h.watch_date FROM watch_history h " +
                     "JOIN movies m ON h.id_movie = m.id JOIN movie_formats f ON h.id_format = f.id " +
                     "WHERE h.id_user = ? ORDER BY h.watch_date DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, Integer.parseInt(uId));
        ResultSet rs = ps.executeQuery();
        boolean first = true;
        while(rs.next()){
            if(!first) sb.append(",");
            sb.append("{\"film\":\"").append(clean(rs.getString(1))).append("\",\"format\":\"")
              .append(clean(rs.getString(2))).append("\",\"data\":\"").append(rs.getString(3)).append("\"}");
            first = false;
        }
    } catch (Exception e) { e.printStackTrace(); }
    sb.append("]");
    byte[] res = sb.toString().getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(200, res.length);
    exchange.getResponseBody().write(res);
    exchange.getResponseBody().close();
});

       server.createContext("/add-comment", (exchange) -> {
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    String q = exchange.getRequestURI().getQuery();
    String response = "OK";
    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
        String uId = q.split("user=")[1].split("&")[0];
        String mId = q.split("movie=")[1].split("&")[0];
        String txt = URLDecoder.decode(q.split("text=")[1].split("&")[0], "UTF-8");
        
        PreparedStatement ps = conn.prepareStatement("INSERT INTO movie_comments (id_user, id_movie, comment_text) VALUES (?, ?, ?)");
        ps.setInt(1, Integer.parseInt(uId));
        ps.setInt(2, Integer.parseInt(mId));
        ps.setString(3, txt);
        ps.executeUpdate();
    } catch (SQLException e) {
        // CERINȚĂ: PRINDERE EXCEPȚIE DIN PL/SQL (Trigger-ul de spam)
        if (e.getErrorCode() == 20001) {
            response = "Eroare PL/SQL: Ai postat prea multe comentarii!";
        } else {
            response = "Eroare baza de date: " + e.getMessage();
        }
    } catch (Exception e) { response = "Eroare Server"; }
    
    exchange.sendResponseHeaders(200, response.length());
    exchange.getResponseBody().write(response.getBytes());
    exchange.getResponseBody().close();
});
      // --- 5. ADĂUGARE/ȘTERGERE DIN WATCHLIST (TOGGLE) ---
        server.createContext("/add-to-watchlist", (exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String q = exchange.getRequestURI().getQuery();
            String response = "Eroare";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                int uId = Integer.parseInt(q.split("user=")[1].split("&")[0]);
                int mId = Integer.parseInt(q.split("movie=")[1].split("&")[0]);

                PreparedStatement check = conn.prepareStatement("SELECT count(*) FROM watchlist WHERE id_user=? AND id_movie=?");
                check.setInt(1, uId);
                check.setInt(2, mId);
                ResultSet rs = check.executeQuery();
                rs.next();

                if (rs.getInt(1) == 0) {
                    // Dacă nu există, îl ADĂUGĂM
                    PreparedStatement ps = conn.prepareStatement("INSERT INTO watchlist (id_user, id_movie) VALUES (?, ?)");
                    ps.setInt(1, uId);
                    ps.setInt(2, mId);
                    ps.executeUpdate();
                    response = "Adaugat";
                } else {
                    // Dacă există deja, îl ȘTERGEM
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM watchlist WHERE id_user=? AND id_movie=?");
                    ps.setInt(1, uId);
                    ps.setInt(2, mId);
                    ps.executeUpdate();
                    response = "Sters";
                }
            } catch (Exception e) { e.printStackTrace(); }
            byte[] bs = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bs.length);
            exchange.getResponseBody().write(bs);
            exchange.getResponseBody().close();
        });
        server.createContext("/get-watchlist", (exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            String q = exchange.getRequestURI().getQuery();
            String uId = (q != null && q.contains("user=")) ? q.split("user=")[1].split("&")[0] : "0";
            StringBuilder sb = new StringBuilder("[");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                // Folosim poster_url pentru imagine
                String sql = "SELECT m.id, m.title, m.poster_url FROM movies m JOIN watchlist w ON m.id = w.id_movie WHERE w.id_user = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, Integer.parseInt(uId));
                ResultSet rs = ps.executeQuery();
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{\"id\":").append(rs.getInt(1))
                      .append(",\"titlu\":\"").append(clean(rs.getString(2)))
                      .append("\",\"imagine\":\"").append(clean(rs.getString(3))).append("\"}");
                    first = false;
                }
            } catch (Exception e) { e.printStackTrace(); }
            sb.append("]");
            byte[] bs = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bs.length);
            exchange.getResponseBody().write(bs);
            exchange.getResponseBody().close();
        });
        // --- 10. ADAUGARE FILM (ADMIN ONLY) ---
        server.createContext("/add-movie", (exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String q = exchange.getRequestURI().getQuery();
            String response = "Eroare";
            
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                // Extragem datele din URL
                String titlu = "", desc = "", an = "", poster = "", trailer = "", cat = "1";
                
                for (String pair : q.split("&")) {
                    String[] kv = pair.split("=");
                    if (kv.length > 1) {
                        String val = URLDecoder.decode(kv[1], "UTF-8");
                        if (kv[0].equals("titlu")) titlu = val;
                        if (kv[0].equals("desc")) desc = val;
                        if (kv[0].equals("an")) an = val;
                        if (kv[0].equals("poster")) poster = val;
                        if (kv[0].equals("trailer")) trailer = val;
                        if (kv[0].equals("cat")) cat = val;
                    }
                }

                String sql = "INSERT INTO movies (title, description, release_year, poster_url, trailer_url, id_category) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, titlu);
                ps.setString(2, desc);
                ps.setInt(3, Integer.parseInt(an));
                ps.setString(4, poster);
                ps.setString(5, trailer);
                ps.setInt(6, Integer.parseInt(cat));
                
                ps.executeUpdate();
                response = "Succes";
            } catch (Exception e) { e.printStackTrace(); }
            
            byte[] bs = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bs.length);
            exchange.getResponseBody().write(bs);
            exchange.getResponseBody().close();
        });
        System.out.println("Server Backend Online pe portul 8081...");
        server.setExecutor(null);
        server.start();
    }
}