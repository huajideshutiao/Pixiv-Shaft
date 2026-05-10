package ceui.lisa.models

import java.io.Serializable

data class WorkspaceBean(
    var pc: String? = null,
    var monitor: String? = null,
    var tool: String? = null,
    var scanner: String? = null,
    var tablet: String? = null,
    var mouse: String? = null,
    var printer: String? = null,
    var desktop: String? = null,
    var music: String? = null,
    var desk: String? = null,
    var chair: String? = null,
    var comment: String? = null,
    var workspace_image_url: String? = null
) : Serializable