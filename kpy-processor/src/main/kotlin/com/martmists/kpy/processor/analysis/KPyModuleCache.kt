package com.martmists.kpy.processor.analysis

import com.martmists.kpy.processor.collected.KPyModule

class KPyModuleCache {
    private val modules = mutableMapOf<String, KPyModule>()

    operator fun get(path: String) : KPyModule {
        return modules.getOrPut(path) {
            KPyModule(path).also {
                if ('.' in path) {
                    val parent = path.split('.').toMutableList().also(MutableList<String>::removeLast).joinToString(".")
                    if (parent.isNotEmpty()) {
                        this[parent].children.add(it)
                    }
                }
            }
        }
    }

    fun all() : List<KPyModule> {
        return modules.values.toList()
    }
}
