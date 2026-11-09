import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class College {

    static ArrayList<Student> students = new ArrayList<>();
    static ArrayList<Course> courses = new ArrayList<>();
    static ArrayList<String> enrollments = new ArrayList<>();

    static class Student {
        int id;
        String name;

        Student(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class Course {
        int id;
        String name;

        Course(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static int studentId = 1;
    static int courseId = 1;

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0);

        server.createContext("/", exchange -> {

            String message = "";
            String search = "";

            if (exchange.getRequestMethod().equals("POST")) {

                String data = new String(
                    exchange.getRequestBody().readAllBytes());

                Map<String, String> form = new HashMap<>();

                for (String pair : data.split("&")) {
                    String[] p = pair.split("=", 2);

                    if (p.length == 2) {
                        form.put(
                            URLDecoder.decode(p[0], "UTF-8"),
                            URLDecoder.decode(p[1], "UTF-8"));
                    }
                }

                String action = form.get("action");

                try {
                    if ("addStudent".equals(action)) {
                        students.add(new Student(
                            studentId++, form.get("name")));
                        message = "Student added successfully!";
                    }

                    else if ("deleteStudent".equals(action)) {
                        int id = Integer.parseInt(form.get("id"));

                        students.removeIf(s -> s.id == id);
                        enrollments.removeIf(e ->
                            e.startsWith(id + "-"));

                        message = "Student deleted!";
                    }

                    else if ("addCourse".equals(action)) {
                        courses.add(new Course(
                            courseId++, form.get("name")));
                        message = "Course added successfully!";
                    }

                    else if ("editCourse".equals(action)) {
                        int id = Integer.parseInt(form.get("id"));
                        Course c = getCourse(id);

                        if (c != null) {
                            c.name = form.get("name");
                            message = "Course updated!";
                        } else {
                            message = "Course not found!";
                        }
                    }

                    else if ("deleteCourse".equals(action)) {
                        int id = Integer.parseInt(form.get("id"));

                        courses.removeIf(c -> c.id == id);
                        enrollments.removeIf(e ->
                            e.endsWith("-" + id));

                        message = "Course deleted!";
                    }

                    else if ("enroll".equals(action)) {
                        int sid = Integer.parseInt(form.get("sid"));
                        int cid = Integer.parseInt(form.get("cid"));

                        Student s = getStudent(sid);
                        Course c = getCourse(cid);

                        if (s == null || c == null) {
                            message = "Invalid student or course ID!";
                        } else if (enrollments.contains(sid + "-" + cid)) {
                            message = "Already enrolled!";
                        } else if (countStudent(sid) >= 5) {
                            message = "Enrollment blocked! Maximum 5 courses allowed.";
                        } else {
                            enrollments.add(sid + "-" + cid);
                            message = "Enrollment successful!";
                        }
                    }

                    else if ("search".equals(action)) {
                        search = form.getOrDefault("search", "");
                    }

                } catch (Exception ex) {
                    message = "Invalid input!";
                }
            }

            String html = createPage(message, search);

            byte[] response = html.getBytes("UTF-8");

            exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8");

            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        server.start();

        System.out.println("College server started!");
        System.out.println("Open http://localhost:8080");
    }

    static Student getStudent(int id) {
        for (Student s : students)
            if (s.id == id)
                return s;
        return null;
    }

    static Course getCourse(int id) {
        for (Course c : courses)
            if (c.id == id)
                return c;
        return null;
    }

    static int countStudent(int id) {
        int count = 0;

        for (String e : enrollments)
            if (e.startsWith(id + "-"))
                count++;

        return count;
    }

    static int countCourse(int id) {
        int count = 0;

        for (String e : enrollments)
            if (e.endsWith("-" + id))
                count++;

        return count;
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static String createPage(String message, String search)
            throws IOException {

        String html = Files.readString(
            Path.of("index.html"));

        StringBuilder studentRows = new StringBuilder();

        for (Student s : students) {
            int count = countStudent(s.id);

            studentRows.append("<tr>")
                .append("<td>").append(s.id).append("</td>")
                .append("<td>").append(esc(s.name)).append("</td>")
                .append("<td>").append(count).append("</td>")
                .append("<td>")
                .append(count > 5 ? "WARNING: OVER LIMIT" : "OK")
                .append("</td><td>")
                .append("<form method='post'>")
                .append("<input type='hidden' name='action' value='deleteStudent'>")
                .append("<input type='hidden' name='id' value='")
                .append(s.id).append("'>")
                .append("<button>Delete</button></form>")
                .append("</td></tr>");
        }

        StringBuilder courseRows = new StringBuilder();

        for (Course c : courses) {

            if (!c.name.toLowerCase().contains(
                    search.toLowerCase()))
                continue;

            courseRows.append("<tr>")
                .append("<td>").append(c.id).append("</td>")
                .append("<td>").append(esc(c.name)).append("</td>")
                .append("<td>").append(countCourse(c.id)).append("</td>")
                .append("<td><form method='post'>")
                .append("<input type='hidden' name='action' value='editCourse'>")
                .append("<input type='hidden' name='id' value='")
                .append(c.id).append("'>")
                .append("<input name='name' value='")
                .append(esc(c.name)).append("' required>")
                .append("<button>Update</button></form></td>")
                .append("<td><form method='post'>")
                .append("<input type='hidden' name='action' value='deleteCourse'>")
                .append("<input type='hidden' name='id' value='")
                .append(c.id).append("'>")
                .append("<button>Delete</button></form></td>")
                .append("</tr>");
        }

        StringBuilder studentOptions = new StringBuilder();
        for (Student s : students) {
            studentOptions.append("<option value='")
                .append(s.id).append("'>")
                .append(s.id).append(" - ")
                .append(esc(s.name)).append("</option>");
        }

        StringBuilder courseOptions = new StringBuilder();
        for (Course c : courses) {
            courseOptions.append("<option value='")
                .append(c.id).append("'>")
                .append(c.id).append(" - ")
                .append(esc(c.name)).append("</option>");
        }

        StringBuilder enrollmentRows = new StringBuilder();

        for (String e : enrollments) {
            String[] ids = e.split("-");
            Student s = getStudent(Integer.parseInt(ids[0]));
            Course c = getCourse(Integer.parseInt(ids[1]));

            if (s != null && c != null) {
                enrollmentRows.append("<tr><td>")
                    .append(esc(s.name)).append("</td><td>")
                    .append(esc(c.name)).append("</td></tr>");
            }
        }

        html = html.replace("{{MESSAGE}}", esc(message));
        html = html.replace("{{STUDENTS}}", studentRows.toString());
        html = html.replace("{{COURSES}}", courseRows.toString());
        html = html.replace("{{STUDENT_OPTIONS}}", studentOptions.toString());
        html = html.replace("{{COURSE_OPTIONS}}", courseOptions.toString());
        html = html.replace("{{ENROLLMENTS}}", enrollmentRows.toString());
        html = html.replace("{{SEARCH}}", esc(search));

        return html;
    }
}