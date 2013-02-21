package org.braindroppings.forkingjava;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * This builds a command and jar for forking a process
 * 
 * @author chinshaw
 */
public class BootstrapBuilder {

    /**
     * This is the created executable jar that will run for our server.
     */
    private File jar;

    private int memLimit = 128;

    private String java = "/usr/bin/java";

    private String extraArgs = "";

    private String extraClasspath = "";

    /**
     * The java environment to use;
     */
    private Map<String, String> environment;

    private static Class<?>[] bootstrapClasses = { IForkServer.class, ForkServer.class, ForkException.class, ForkTimeoutException.class, IForkClient.class, Constants.class,
            IForkService.class, IForkedJob.class, Void.class, ForkServer.PingThread.class, RemoteOperation.class };

    public BootstrapBuilder() {
    }

    public BootstrapBuilder(String java) {
        this.java = java;
    }

    public List<String> build() throws BootstrapException {
        try {
            this.jar = createBootstrapJar();
        } catch (IOException e) {
            throw new BootstrapException("Unable to create bootstrap jar to execute ", e);
        }

        if (java == null) {
            throw new BootstrapException("java executable is a required argument use setJavaExecutable");
        }

        List<String> command = new ArrayList<String>();

        command.addAll(Arrays.asList(java.split("\\s+")));
        command.add("-Djava.security.policy=/Users/chinshaw/devel/workspace/vf-trunk/taskengine/src/META-INF/taskenginesecurity.policy");
        command.add("-Xmx" + memLimit + "M");
        command.add("-jar");
        command.add(jar.getPath());
        command.addAll(Arrays.asList(extraArgs.split("\\s+")));

        return command;
    }

    public BootstrapBuilder setJavaExecutable(String javaExecutablePath) {
        this.java = javaExecutablePath;
        return this;
    }

    public BootstrapBuilder addExtraArgument(String argument) {
        extraArgs += argument + " ";
        return this;
    }

    public BootstrapBuilder addJar(String jarClassPath) {
        extraClasspath += "jarClassPath" + " ";
        return this;
    }

    public BootstrapBuilder setMaxHeapSize(int maxMemory) {
        this.memLimit = maxMemory;
        return this;
    }

    public BootstrapBuilder addClasspath(String classPath) {
        extraClasspath += "jarClassPath" + " ";
        return this;
    }

    public BootstrapBuilder setEnvironment(Map<String, String> environment) {
        this.environment = environment;
        return this;
    }

    /**
     * Fills in the jar file used to bootstrap the forked server process. All
     * the required <code>.class</code> files and a manifest with a
     * <code>Main-Class</code> entry are written into the archive.
     * 
     * @param file
     *            file to hold the bootstrap archive
     * @throws IOException
     *             if the bootstrap archive could not be created
     */
    private void fillBootstrapJar(File file, Class<?>[] bootstrap) throws IOException {
        JarOutputStream jar = new JarOutputStream(new FileOutputStream(file));
        try {
            String manifest = "Main-Class: " + ForkServer.class.getName() + "\n";

            

            jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            jar.write(manifest.getBytes("UTF-8"));
            
            if (extraClasspath != null && !(extraClasspath.isEmpty())) {
                List<String> chunkedPath = chunkedClasspath(extraClasspath, 60);
                jar.write("Class-Path: ".getBytes("UTF-8"));
               

                
                for (Iterator<String> iter = chunkedPath.iterator(); iter.hasNext();) {
                    String chunk = iter.next();
                    jar.write(" ".getBytes("UTF-8"));
                    jar.write(chunk.getBytes("UTF-8"));
                    jar.write("\n".getBytes("UTF-8"));
                    
                }
               // jar.write("\n".getBytes("UTF-8"));
            }
            

            ClassLoader loader = ForkServer.class.getClassLoader();
            for (Class<?> klass : bootstrap) {
                String path = klass.getName().replace('.', '/') + ".class";
                InputStream input = loader.getResourceAsStream(path);
                try {
                    jar.putNextEntry(new JarEntry(path));
                 //   IOUtils.copy(input, jar);
                } finally {
                    input.close();
                }
            }
        } finally {
            jar.close();
        }
    }

    /**
     * Creates a temporary jar file that can be used to bootstrap the forked
     * server process. Remember to remove the file when no longer used.
     * 
     * @return the created jar file
     * @throws IOException
     *             if the bootstrap archive could not be created
     */
    private File createBootstrapJar() throws IOException {
        File file = File.createTempFile("forked-job-", ".jar");
        boolean ok = false;
        try {
            fillBootstrapJar(file, bootstrapClasses);
            ok = true;
        } finally {
            if (!ok) {
                file.delete();
            }
        }
        return file;
    }

    public BootstrapBuilder setExtraClassPath(List<String> extraClasspath) {
        this.extraClasspath = "";
        for (String path : extraClasspath) {
            this.extraClasspath += path + " ";
        }
        return this;
    }

    public BootstrapBuilder setExtraClassPath(String extraClasspath) {
        setExtraClassPath(Arrays.asList(extraClasspath.split(":")));
        
        return this;
    }
    
    
    
    private static List<String> chunkedClasspath(String classPath, int chunkSize) {
        List<String> chunked = new ArrayList<String>();
        int index = 0;
        while (index < classPath.length()) {
            chunked.add((classPath.substring(index, Math.min(index+chunkSize,classPath.length()))));
            index += chunkSize;
        }
        return chunked;
    }
}