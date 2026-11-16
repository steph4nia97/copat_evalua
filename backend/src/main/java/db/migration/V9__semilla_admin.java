package db.migration;

import java.sql.PreparedStatement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class V9__semilla_admin extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    try (PreparedStatement ps = context.getConnection().prepareStatement(
        """
        INSERT INTO usuario (email, password_hash, nombre, departamento_id, clave_provisoria)
        VALUES (?, ?, ?, 4, FALSE)
        """)) {
      ps.setString(1, "admin@copat.cl");
      ps.setString(2, encoder.encode("admin123"));
      ps.setString(3, "Admin Copat");
      ps.executeUpdate();
    }
  }
}
