BIBLIOTECA ESCOLAR — EVALUACIÓN FINAL TRANSVERSAL SEMANA 9

Aplicación de escritorio Java 17, Maven, Swing y MySQL. GroupId cl.biblioteca;
ArtifactId biblioteca-escolar. No requiere librerías gráficas externas.

CONFIGURACIÓN Y EJECUCIÓN

1. Mantener MySQL activo en localhost:3306 y la base biblioteca ya preparada.
   NO ejecutar los scripts de sql/: se conservan únicamente para la entrega.
2. Configurar BIBLIOTECA_DB_USER y BIBLIOTECA_DB_PASSWORD en la configuración
   Run de IntelliJ (Edit Configurations > Environment variables).
   Como alternativa, copiar config-local.example.properties a
   config-local.properties y completar sus credenciales. Ese archivo está
   excluido de Git y del ZIP. Las variables de entorno tienen prioridad.
   La configuración local se lee desde el directorio de trabajo.
3. En IntelliJ, recargar Maven y ejecutar cl.biblioteca.Main con JDK 17.
   Para compilar desde la terminal: mvn package
   Si Maven no está en PATH, usar el panel Maven de IntelliJ > Lifecycle > package.
4. Desde la carpeta raíz del proyecto, ejecutar:
   java -jar target/biblioteca-escolar-1.0-SNAPSHOT.jar
   En el ZIP preparado, el mismo JAR se encuentra en target/.

El JAR incluye Connector/J y sus dependencias. No contiene credenciales MySQL.
No distribuir config-local.properties. Cada evaluador debe configurar su conexión.

ACCESO ACADÉMICO

Inicio de sesión por RUT sin puntos, con guion, y contraseña del script original.
Bibliotecario: 12345678-9 / clave123.
Estudiante: 98765432-1 / clave123.
Se conserva el formato académico de contraseñas; no se modifican usuarios.

Bibliotecario: gestión completa de libros, estudiantes y categorías; préstamos,
devoluciones y los tres informes.
Estudiante: consulta de libros; préstamos, devoluciones e historial propios,
vinculados por el RUT existente entre usuarios y estudiantes. No administra CRUD.
El informe de libros más prestados muestra el catálogo agregado; préstamos
actuales e historial se restringen a los del estudiante autenticado.

OPERACIÓN

En cada CRUD, Nuevo limpia la selección. Guardar crea un registro nuevo o modifica
el seleccionado. Eliminar solicita confirmación y MySQL impide borrar registros
referenciados. Actualizar lista recarga los datos y las categorías disponibles.
Los ISBN y RUT únicos se respetan mediante las restricciones originales de MySQL.
El stock debe ser un entero no negativo.

Préstamos: seleccionar estudiante y libro, y registrar. Vencimiento = fecha actual
más siete días corridos, consistente con los ejemplos del docente. El stock es el
valor disponible almacenado en libros; no se recalculan los datos originales.
Devoluciones: seleccionar un préstamo activo y devolver. El vencimiento previsto
se conserva. Un préstamo vencido y no devuelto aparece como Atrasado.
El historial conserva cada préstamo en la misma tabla prestamos.

Reportes: exactamente libros más prestados, historial por estudiante y libros
actualmente en préstamo, todos mediante JTable.

ORGANIZACIÓN DEL CÓDIGO

modelo: entidades encapsuladas; Persona abstracta y sus subclases Usuario y
Estudiante muestran herencia y distintas implementaciones de getDescripcion.
dao: CrudDAO<T> define la interfaz común; LibroDAO, EstudianteDAO y CategoriaDAO
implementan CRUD. UsuarioDAO autentica, PrestamoDAO administra las transacciones,
y ReporteDAO concentra las tres consultas. Las ventanas no contienen SQL.
controlador: LoginControlador, BibliotecaControlador y PrestamoControlador
conectan vistas y DAO y comprueban permisos y reglas.
vista: componentes Swing creados directamente en Java, sin archivos .form.
conexion: DatabaseConnection tiene constructor privado y getInstance; administra
conexiones nuevas mediante DriverManager.getConnection. Cada operación las cierra.
Main: crea el acceso en el hilo de eventos de Swing.

CONCURRENCIA

TareasSwing usa SwingWorker: doInBackground hace JDBC y done actualiza Swing en EDT.
PrestamoControlador sincroniza la sección crítica con un bloqueo estático compartido.
PrestamoDAO reduce stock con UPDATE ... WHERE stock > 0 e inserta el préstamo en una
única transacción. MySQL protege el stock también entre conexiones independientes.
Para devolver, SELECT ... FOR UPDATE bloquea el préstamo, verifica devuelto y
actualiza préstamo y stock antes del commit. Los errores ejecutan rollback.
Se comprobó que las cinco tablas locales usan InnoDB; no se altera su motor.

PRUEBAS

Las clases de verificación son programas Java de integración, sin dependencias
adicionales de pruebas. mvn package compila, pero no ejecuta automáticamente estos
programas. Para ejecutarlos desde la raíz:

mvn test-compile package
java -cp target/test-classes:target/biblioteca-escolar-1.0-SNAPSHOT.jar cl.biblioteca.VerificacionLectura
java -cp target/test-classes:target/biblioteca-escolar-1.0-SNAPSHOT.jar cl.biblioteca.VerificacionSwing

Lectura comprueba conexión real, dos conexiones independientes, Singleton,
autenticación de ambos roles, permisos del controlador y los tres reportes.
Swing verifica tareas JDBC fuera de EDT, callback en EDT, carga de datos, menús por
rol y genera imágenes de los componentes en target/verificacion-visual.

VerificacionEscritura requiere autorización para crear y luego limpiar sus propios
datos nuevos. Nunca elimina los 45 registros originales. Avanzan los contadores
AUTO_INCREMENT. Solo después de autorizar esa prueba:

java -cp target/test-classes:target/biblioteca-escolar-1.0-SNAPSHOT.jar cl.biblioteca.VerificacionEscritura --autorizar-datos-nuevos

Para comprobación manual, seguir docs/PRUEBAS_MANUALES.txt. El estado y las pruebas
realmente realizadas están en docs/REVISION_REQUISITOS.txt.

ENTREGA Y VIDEO

Los dos scripts originales están en sql/, sin modificaciones ni ejecución.
El ZIP local es una preparación de archivos; no se ha publicado ni entregado.
No se hicieron commits, push ni creación de un repositorio público.
Publicar en GitHub y entregar requieren autorización del estudiante.
Guion de 3 a 5 minutos en docs/GUION_VIDEO.txt. Grabar en Kaltura y aparecer con
el rostro visible durante alguna parte del video.
