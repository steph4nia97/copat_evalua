package db.migration;

import java.sql.PreparedStatement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class V2__semilla_usuario extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    String hash = new BCryptPasswordEncoder().encode("semilla1");
    try (PreparedStatement ps = context.getConnection().prepareStatement(
        """
        INSERT INTO usuario (email, password_hash, nombre, departamento_id)
        VALUES (?, ?, ?, 1)
        """)) {
      ps.setString(1, "semilla1@copat.cl");
      ps.setString(2, hash);
      ps.setString(3, "Semilla Uno");
      ps.executeUpdate();
    }
  }
}
