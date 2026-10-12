package cl.biblioteca.modelo;

public abstract class Persona {
    private int id;
    private String nombre;
    private String rut;
    private String correo;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    /** Cada tipo de persona proporciona su identificación en la interfaz. */
    public abstract String getDescripcion();
}
