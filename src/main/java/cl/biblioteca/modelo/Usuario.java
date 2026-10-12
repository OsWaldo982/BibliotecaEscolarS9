package cl.biblioteca.modelo;

public class Usuario extends Persona {
    private String rol;

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean esBibliotecario() { return "bibliotecario".equals(rol); }
    @Override
    public String getDescripcion() { return getNombre() + " (" + rol + ")"; }
}
