package systems.symbol.servlet;

import org.apache.camel.http.common.CamelServlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class IQServlet extends HttpServlet {

public void doGet(HttpServletRequest req, HttpServletResponse res) {
try {
res.setStatus(200);
res.getWriter().println("OK");
} catch (IOException e) {
res.setStatus(500);
e.printStackTrace();
}
}
}
