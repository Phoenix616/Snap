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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.md_5.bungee.api.ProxyServer;

final class PluginClassloader extends URLClassLoader {

    private static final Set<PluginClassloader> allLoaders = new CopyOnWriteArraySet<>();
    //
    private final ProxyServer proxy;
    private final PluginDescription desc;
    //
    private Plugin plugin;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public PluginClassloader(ProxyServer proxy, PluginDescription desc, URL[] urls) {
        super(urls, proxy.getClass().getClassLoader());
        this.proxy = proxy;
        this.desc = desc;

        allLoaders.add(this);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true);
    }

    private Class<?> loadClass0(String name, boolean resolve, boolean checkOther) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException ex) {
        }
        if (checkOther) {
            for (PluginClassloader loader : allLoaders) {
                if (loader != this) {
                    try {
                        return loader.loadClass0(name, resolve, false);
                    } catch (ClassNotFoundException ex) {
                    }
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

    void init(Plugin plugin) {
        Preconditions.checkArgument(plugin != null, "plugin");
        Preconditions.checkArgument(plugin.getClass().getClassLoader() == this, "Plugin has incorrect ClassLoader");
        if (this.plugin != null) {
            throw new IllegalArgumentException("Plugin already initialized!");
        }

        this.plugin = plugin;
        plugin.init(proxy, desc);
    }
}