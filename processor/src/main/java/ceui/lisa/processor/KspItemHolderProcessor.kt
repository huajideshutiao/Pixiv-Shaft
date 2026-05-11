package ceui.lisa.processor

import ceui.lisa.annotations.ItemHolder
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import java.io.OutputStream

class KspItemHolderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("ceui.lisa.annotations.ItemHolder")
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        if (symbols.isEmpty()) return emptyList()

        val holderEntries = mutableListOf<HolderEntry>()
        symbols.forEach { symbol ->
            if (symbol.validate()) {
                val annotation = symbol.getAnnotationsByType(ItemHolder::class).firstOrNull()
                if (annotation != null) {
                    val ksAnnotation = symbol.annotations.find {
                        it.shortName.asString() == "ItemHolder"
                    }
                    val itemHolderType = ksAnnotation?.arguments?.find {
                        it.name?.asString() == "itemHolderCls" || it.name == null
                    }?.value as? KSType

                    val itemHolderFullName =
                        itemHolderType?.declaration?.qualifiedName?.asString() ?: ""

                    if (itemHolderFullName.isEmpty()) {
                        logger.warn("ItemHolder type not resolved for ${symbol.simpleName.asString()}")
                        return@forEach
                    }

                    val itemHolderName = itemHolderFullName.split(".").last()

                    val primaryConstructor = symbol.primaryConstructor
                    val bindingType = primaryConstructor?.parameters?.firstOrNull()?.type?.resolve()
                    val bindingFullName = bindingType?.declaration?.qualifiedName?.asString() ?: ""

                    if (bindingFullName.isEmpty()) {
                        logger.warn("Binding type not resolved for ${symbol.simpleName.asString()}")
                        return@forEach
                    }

                    val bindingName = bindingFullName.split(".").last()

                    holderEntries.add(
                        HolderEntry(
                            existingPackage = symbol.packageName.asString() + ".",
                            itemHolder = itemHolderName,
                            binding = bindingName,
                            bindingFullname = bindingFullName,
                            viewHolder = symbol.simpleName.asString()
                        )
                    )
                }
            }
        }

        if (holderEntries.isNotEmpty()) {
            generateFile(holderEntries)
        }

        return emptyList()
    }

    private fun generateFile(holderEntries: List<HolderEntry>) {
        val packageName = "ceui.pixiv.ui.viewholdermap"
        val fileName = "ViewHolderFactory"
        val file: OutputStream = codeGenerator.createNewFile(
            Dependencies(false),
            packageName,
            fileName
        )

        val content = StringBuilder()
        content.append("package $packageName\n\n")
        content.append("import android.view.LayoutInflater\n")
        content.append("import android.view.ViewGroup\n")
        content.append("import androidx.viewbinding.ViewBinding\n")
        content.append("import ceui.pixiv.ui.common.ListItemHolder\n")
        content.append("import ceui.pixiv.ui.common.ListItemViewHolder\n")

        holderEntries.forEach {
            content.append("import ${it.existingPackage}${it.viewHolder}\n")
            content.append("import ${it.existingPackage}${it.itemHolder}\n")
        }

        holderEntries.filter { it.binding.endsWith("Binding") }
            .map { it.bindingFullname }
            .distinct()
            .forEach { content.append("import $it\n") }

        content.append("\nobject ViewHolderFactory {\n")

        holderEntries.forEach {
            content.append("\n    private fun ${it.viewHolder}Builder(parent: ViewGroup): ListItemViewHolder<out ViewBinding, out ListItemHolder> {\n")
            content.append("        val binding = ${it.binding}.inflate(\n")
            content.append("            LayoutInflater.from(parent.context),\n")
            content.append("            parent,\n")
            content.append("            false\n")
            content.append("        )\n")
            content.append("        return ${it.viewHolder}(binding)\n")
            content.append("    }\n")
        }

        val buildMapEntries = holderEntries.map {
            "    ${it.itemHolder}::class.java.hashCode() to ViewHolderFactory::${it.viewHolder}Builder"
        }

        content.append("\n    val VIEW_HOLDER_MAP = mapOf(\n")
        content.append(buildMapEntries.joinToString(",\n"))
        content.append("\n    )\n")
        content.append("}\n")

        file.write(content.toString().toByteArray())
        file.close()
    }

    data class HolderEntry(
        val existingPackage: String,
        val itemHolder: String,
        val binding: String,
        val bindingFullname: String,
        val viewHolder: String,
    )
}

class KspItemHolderProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KspItemHolderProcessor(environment.codeGenerator, environment.logger)
    }
}
