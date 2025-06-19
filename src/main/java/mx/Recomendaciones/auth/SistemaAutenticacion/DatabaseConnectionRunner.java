package mx.Recomendaciones.auth.SistemaAutenticacion;


import mx.Recomendaciones.auth.service.DatabaseConnectionTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionRunner implements CommandLineRunner {

    @Autowired
    private DatabaseConnectionTest connectionTest;

    @Override
    public void run(String... args) throws Exception {
        connectionTest.checkConnection();
    }
}

