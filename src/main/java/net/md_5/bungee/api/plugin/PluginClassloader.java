/*
 * Copyright (c) 2012, md_5. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * The name of the author may not be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * You may not use the software for commercial software hosting services without
 * written permission from the author.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.md_5.bungee.api.plugin;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import net.md_5.bungee.api.ProxyServer;

final class PluginClassloader extends URLClassLoader {

    private static final Set<PluginClassloader> allLoaders = new CopyOnWriteArraySet<>();
    //
    private final ProxyServer proxy;
    private final PluginDescription desc;
    private final JarFile jar;
    private final Manifest manifest;
    private final URL url;
    private final ClassLoader libraryLoader;
    //
    private Plugin plugin;

    public PluginClassloader(ProxyServer proxy, PluginDescription desc, File file, ClassLoader libraryLoader) throws IOException {
        super(new URL[]{file.toURI().toURL()}, proxy.getClass().getClassLoader()); // Snap - Add parent class loader
        this.proxy = proxy;
        this.desc = desc;
        this.jar = new JarFile(file);
        this.manifest = this.jar.getManifest();
        this.url = file.toURI().toURL();
        this.libraryLoader = libraryLoader;
        allLoaders.add(this);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return this.loadClass0(name, resolve, true, true);
    }

    private Class<?> loadClass0(String name, boolean resolve, boolean checkOther, boolean checkLibraries) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException var10) {
            if (checkLibraries && this.libraryLoader != null) {
                try {
                    return this.libraryLoader.loadClass(name);
                } catch (ClassNotFoundException var9) {
                    ;
                }
            }

            if (checkOther) {
                Iterator var5 = allLoaders.iterator();

                while(true) {
                    PluginClassloader loader;
                    do {
                        if (!var5.hasNext()) {
                            throw new ClassNotFoundException(name);
                        }

                        loader = (PluginClassloader)var5.next();
                    } while(loader == this);

                    try {
                        return loader.loadClass0(name, resolve, false, this.proxy.getPluginManager().isTransitiveDepend(this.desc, loader.desc));
                    } catch (ClassNotFoundException var8) {
                        ;
                    }
                }
            } else {
                throw new ClassNotFoundException(name);
            }
        }
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        JarEntry entry = this.jar.getJarEntry(path);
        if (entry != null) {
            byte[] classBytes;
            try {
                InputStream is = this.jar.getInputStream(entry);

                try {
                    classBytes = ByteStreams.toByteArray(is);
                } catch (Throwable var9) {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Throwable var8) {
                            var9.addSuppressed(var8);
                        }
                    }

                    throw var9;
                }

                if (is != null) {
                    is.close();
                }
            } catch (IOException var10) {
                throw new ClassNotFoundException(name, var10);
            }

            int dot = name.lastIndexOf(46);
            if (dot != -1) {
                String pkgName = name.substring(0, dot);
                if (this.getPackage(pkgName) == null) {
                    try {
                        if (this.manifest != null) {
                            this.definePackage(pkgName, this.manifest, this.url);
                        } else {
                            this.definePackage(pkgName, (String)null, (String)null, (String)null, (String)null, (String)null, (String)null, (URL)null);
                        }
                    } catch (IllegalArgumentException var11) {
                        if (this.getPackage(pkgName) == null) {
                            throw new IllegalStateException("Cannot find package " + pkgName);
                        }
                    }
                }
            }

            CodeSigner[] signers = entry.getCodeSigners();
            CodeSource source = new CodeSource(this.url, signers);
            return this.defineClass(name, classBytes, 0, classBytes.length, source);
        } else {
            return super.findClass(name);
        }
    }

    public void close() throws IOException {
        try {
            super.close();
        } finally {
            this.jar.close();
        }

    }

    void init(Plugin plugin) {
        Preconditions.checkArgument(plugin != null, "plugin");
        Preconditions.checkArgument(plugin.getClass().getClassLoader() == this, "Plugin has incorrect ClassLoader");
        if (this.plugin != null) {
            throw new IllegalArgumentException("Plugin already initialized!");
        } else {
            this.plugin = plugin;
            plugin.init(this.proxy, this.desc);
        }
    }

    public String toString() {
        return "PluginClassloader(desc=" + this.desc + ")";
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}
