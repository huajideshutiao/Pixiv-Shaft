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
import java.io.OutputStream

class KspItemHolderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var invoked = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }

        val symbols = resolver.getSymbolsWithAnnotation("ceui.lisa.annotations.ItemHolder")
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        if (symbols.isEmpty()) {
            return emptyList()
        }

        logger.info("KspItemHolderProcessor: Found ${symbols.size} @ItemHolder annotated classes")

        val holderEntries = mutableListOf<HolderEntry>()
        symbols.forEach { symbol ->
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

                val primaryConstructor = symbol.primaryConstructor
                val bindingType = primaryConstructor?.parameters?.firstOrNull()?.type?.resolve()
                val bindingFullName = bindingType?.declaration?.qualifiedName?.asString() ?: ""

                if (itemHolderFullName.isNotEmpty() && bindingFullName.isNotEmpty()) {
                    val bindingName = bindingFullName.split(".").last()
                    holderEntries.add(
                        HolderEntry(
                            itemHolderFullName = itemHolderFullName,
                            bindingName = bindingName,
                            bindingFullname = bindingFullName,
                            viewHolderFullName = symbol.qualifiedName?.asString() ?: ""
                        )
                    )
                } else {
                    logger.warn("KspItemHolderProcessor: Could not resolve types for ${symbol.simpleName.asString()}. itemHolder: $itemHolderFullName, binding: $bindingFullName")
                }
            }
        }

        generateFile(holderEntries)
        invoked = true

        return emptyList()
    }

    private fun generateFile(holderEntries: List<HolderEntry>) {
        val packageName = "ceui.pixiv.ui.viewholdermap"
        val fileName = "ViewHolderFactory"
        val file: OutputStream = codeGenerator.createNewFile(
            Dependencies(true),
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

        val imports = mutableSetOf<String>()
        holderEntries.forEach {
            imports.add(it.viewHolderFullName)
            imports.add(it.itemHolderFullName)
        }
        
        holderEntries.filter { it.bindingName.endsWith("Binding") }
            .forEach { imports.add(it.bindingFullname) }
            
        imports.filter { it.isNotEmpty() }.sorted().forEach {
            content.append("import $it\n")
        }

        content.append("\nobject ViewHolderFactory {\n")

        content.append("\n    fun createViewHolder(viewType: Int, parent: ViewGroup): ListItemViewHolder<out ViewBinding, out ListItemHolder>? {\n")
        content.append("        return when (viewType) {\n")

        holderEntries.forEach {
            val viewHolderName = it.viewHolderFullName.split(".").last()
            content.append("            \"${it.itemHolderFullName}\".hashCode() -> {\n")
            content.append("                val binding = ${it.bindingName}.inflate(\n")
            content.append("                    LayoutInflater.from(parent.context),\n")
            content.append("                    parent,\n")
            content.append("                    false\n")
            content.append("                )\n")
            content.append("                $viewHolderName(binding)\n")
            content.append("            }\n")
        }

        content.append("            else -> null\n")
        content.append("        }\n")
        content.append("    }\n")
        content.append("}\n")

        file.write(content.toString().toByteArray())
        file.close()
    }

    data class HolderEntry(
        val itemHolderFullName: String,
        val bindingName: String,
        val bindingFullname: String,
        val viewHolderFullName: String,
    )
}

class KspItemHolderProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KspItemHolderProcessor(environment.codeGenerator, environment.logger)
    }
}
