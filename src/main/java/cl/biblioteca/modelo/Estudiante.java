package cl.biblioteca.modelo;

public class Estudiante extends Persona {
    private String curso;

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    @Override
    public String getDescripcion() { return getNombre() + " - " + curso; }
    @Override
    public String toString() { return getDescripcion(); }
}
