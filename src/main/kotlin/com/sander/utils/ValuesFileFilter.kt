package com.sander.utils

import org.apache.commons.io.filefilter.AbstractFileFilter
import java.io.File

class ValuesFileFilter : AbstractFileFilter() {

    override fun accept(dir: File?, name: String?): Boolean {
        if (dir == null || name == null) {
            return false
        }
        return name.startsWith("values") && name.endsWith(".xml")
                && "${dir.name}.xml" == name
    }
}