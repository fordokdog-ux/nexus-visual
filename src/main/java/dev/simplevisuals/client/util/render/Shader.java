package dev.simplevisuals.client.util.render;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.Objects;
import java.util.stream.Collectors;

public class Shader {

    private static final int VERTEX_SHADER;
    private int programId;

    static {
        VERTEX_SHADER = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(VERTEX_SHADER, getShaderSource("vertex.vert"));
        GL20.glCompileShader(VERTEX_SHADER);

        if (GL20.glGetShaderi(VERTEX_SHADER, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Vertex shader compilation failed:\n" + GL20.glGetShaderInfoLog(VERTEX_SHADER));
        }
    }

    public Shader(String fragmentShaderName) {
        programId = GL20.glCreateProgram();

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, getShaderSource(fragmentShaderName));
        GL20.glCompileShader(fragmentShader);

        if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Fragment shader compilation failed:\n" + GL20.glGetShaderInfoLog(fragmentShader));
            GL20.glDeleteShader(fragmentShader);
        }

        GL20.glAttachShader(programId, VERTEX_SHADER);
        GL20.glAttachShader(programId, fragmentShader);
        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("Shader program linking failed:\n" + GL20.glGetProgramInfoLog(programId));
        }

        GL20.glDeleteShader(fragmentShader);
    }

    public void load() {
        GL20.glUseProgram(programId);
    }

    public void unload() {
        GL20.glUseProgram(0);
    }

    public int getUniform(String name) {
        return GL20.glGetUniformLocation(programId, name);
    }

    public void setUniformf(String name, float... args) {
        int loc = getUniform(name);
        switch (args.length) {
            case 1 -> GL20.glUniform1f(loc, args[0]);
            case 2 -> GL20.glUniform2f(loc, args[0], args[1]);
            case 3 -> GL20.glUniform3f(loc, args[0], args[1], args[2]);
            case 4 -> GL20.glUniform4f(loc, args[0], args[1], args[2], args[3]);
        }
    }

    public void setUniformi(String name, int... args) {
        int loc = getUniform(name);
        switch (args.length) {
            case 1 -> GL20.glUniform1i(loc, args[0]);
            case 2 -> GL20.glUniform2i(loc, args[0], args[1]);
            case 3 -> GL20.glUniform3i(loc, args[0], args[1], args[2]);
            case 4 -> GL20.glUniform4i(loc, args[0], args[1], args[2], args[3]);
        }
    }

    public void setUniformfb(String name, FloatBuffer buffer) {
        int loc = getUniform(name);
        GL20.glUniform1fv(loc, buffer);
    }

    public static void draw() {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        draw(0, 0, width, height);
    }

    public static void draw(double x, double y, double width, double height) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(0, 0); GL11.glVertex2d(x, y);
        GL11.glTexCoord2d(0, 1); GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2d(1, 1); GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2d(1, 0); GL11.glVertex2d(x + width, y);
        GL11.glEnd();
    }

    private static String getShaderSource(String fileName) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(
                        Shader.class.getResourceAsStream("/assets/renderutil/shaders/" + fileName))))) {

            return reader.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .map(line -> line.replace("\t", ""))
                    .collect(Collectors.joining("\n"));

        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader source: " + fileName, e);
        }
    }
}
