package db.migration;

import java.sql.PreparedStatement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class V6__semilla_departamentos extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    insertar(context, "semilla2@copat.cl", encoder.encode("semilla2"), "Semilla Dos", 2);
    insertar(context, "semilla3@copat.cl", encoder.encode("semilla3"), "Semilla Tres", 3);
  }

  private static void insertar(Context context, String email, String hash, String nombre, int departamento)
      throws Exception {
    try (PreparedStatement ps = context.getConnection().prepareStatement(
        """
        INSERT INTO usuario (email, password_hash, nombre, departamento_id)
        VALUES (?, ?, ?, ?)
        """)) {
      ps.setString(1, email);
      ps.setString(2, hash);
      ps.setString(3, nombre);
      ps.setInt(4, departamento);
      ps.executeUpdate();
    }
  }
}
