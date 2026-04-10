package app.cclauncher.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

 object HomeItemAppSerializer : KSerializer<HomeItem.App> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HomeItem.App") {
        element<String>("id")
        element<Int>("page")
        element<Int>("row")
        element<Int>("column")
        element<Int>("rowSpan")
        element<Int>("columnSpan")
        element<String>("appLabel")
        element<String>("appPackage")
        element<String>("activityClassName")
        element<String>("userString")
        element<Boolean>("isHidden")
        element<Boolean>("isSystemShortcut")
        element<String>("systemShortcutId")
        element<String>("systemShortcutPackage")
        element<Float>("appTextSize")
        element<Int>("appLabelAlignment")
    }

    override fun serialize(encoder: Encoder, value: HomeItem.App) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.id)
            encodeIntElement(descriptor, 1, value.page)
            encodeIntElement(descriptor, 2, value.row)
            encodeIntElement(descriptor, 3, value.column)
            encodeIntElement(descriptor, 4, value.rowSpan)
            encodeIntElement(descriptor, 5, value.columnSpan)
            encodeStringElement(descriptor, 6, value.appModel.appLabel)
            encodeStringElement(descriptor, 7, value.appModel.appPackage)
            encodeStringElement(descriptor, 8, value.appModel.activityClassName.orEmpty())
            encodeStringElement(descriptor, 9, value.appModel.userString)
            encodeBooleanElement(descriptor, 10, value.appModel.isHidden)
            encodeBooleanElement(descriptor, 11, value.appModel.isSystemShortcut)
            encodeStringElement(descriptor, 12, value.appModel.systemShortcutId.orEmpty())
            encodeStringElement(descriptor, 13, value.appModel.systemShortcutPackage.orEmpty())
            encodeFloatElement(descriptor, 14, value.appTextSize)
            encodeIntElement(descriptor, 15, value.appLabelAlignment)
        }
    }

    override fun deserialize(decoder: Decoder): HomeItem.App {
        var id = ""
        var page = 0
        var row = 0
        var column = 0
        var rowSpan = 1
        var columnSpan = 1
        var appLabel = ""
        var appPackage = ""
        var activityClassNameRaw = ""
        var userString = ""
        var isHidden = false
        var isSystemShortcut = false
        var systemShortcutId = ""
        var systemShortcutPackage = ""
        var appTextSize = 1.0f
        var appLabelAlignment = -1

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeStringElement(descriptor, index)
                    1 -> page = decodeIntElement(descriptor, index)
                    2 -> row = decodeIntElement(descriptor, index)
                    3 -> column = decodeIntElement(descriptor, index)
                    4 -> rowSpan = decodeIntElement(descriptor, index)
                    5 -> columnSpan = decodeIntElement(descriptor, index)
                    6 -> appLabel = decodeStringElement(descriptor, index)
                    7 -> appPackage = decodeStringElement(descriptor, index)
                    8 -> activityClassNameRaw = decodeStringElement(descriptor, index)
                    9 -> userString = decodeStringElement(descriptor, index)
                    10 -> isHidden = decodeBooleanElement(descriptor, index)
                    11 -> isSystemShortcut = decodeBooleanElement(descriptor, index)
                    12 -> systemShortcutId = decodeStringElement(descriptor, index)
                    13 -> systemShortcutPackage = decodeStringElement(descriptor, index)
                    14 -> appTextSize = decodeFloatElement(descriptor, index)
                    15 -> appLabelAlignment = decodeIntElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> {
                        decodeStringElement(descriptor, index)
                    }
                }
            }
        }

        val activityClassName = activityClassNameRaw
            .trim()
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

        val appModel = AppModel(
            appLabel = appLabel,
            appPackage = appPackage,
            activityClassName = activityClassName,
            isHidden = isHidden,
            userString = userString,
            isSystemShortcut = isSystemShortcut,
            systemShortcutId = systemShortcutId.takeIf { it.isNotEmpty() },
            systemShortcutPackage = systemShortcutPackage.takeIf { it.isNotEmpty() }
        )

        return HomeItem.App(
            id = id,
            appModel = appModel,
            appTextSize = appTextSize,
            appLabelAlignment = appLabelAlignment,
            page = page,
            row = row,
            column = column,
            rowSpan = rowSpan,
            columnSpan = columnSpan
        )
    }
}

object HomeItemWidgetSerializer : KSerializer<HomeItem.Widget> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HomeItem.Widget") {
        element<String>("id")
        element<Int>("page")
        element<Int>("appWidgetId")
        element<String>("packageName")
        element<String>("providerClassName")
        element<Int>("row")
        element<Int>("column")
        element<Int>("rowSpan")
        element<Int>("columnSpan")
    }

    override fun serialize(encoder: Encoder, value: HomeItem.Widget) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.id)
            encodeIntElement(descriptor, 1, value.page)
            encodeIntElement(descriptor, 2, value.appWidgetId)
            encodeStringElement(descriptor, 3, value.packageName)
            encodeStringElement(descriptor, 4, value.providerClassName)
            encodeIntElement(descriptor, 5, value.row)
            encodeIntElement(descriptor, 6, value.column)
            encodeIntElement(descriptor, 7, value.rowSpan)
            encodeIntElement(descriptor, 8, value.columnSpan)
        }
    }

    override fun deserialize(decoder: Decoder): HomeItem.Widget {
        var id = ""
        var page = 0
        var appWidgetId = -1
        var packageName = ""
        var providerClassName = ""
        var row = 0
        var column = 0
        var rowSpan = 1
        var columnSpan = 1

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeStringElement(descriptor, index)
                    1 -> page = decodeIntElement(descriptor, index)
                    2 -> appWidgetId = decodeIntElement(descriptor, index)
                    3 -> packageName = decodeStringElement(descriptor, index)
                    4 -> providerClassName = decodeStringElement(descriptor, index)
                    5 -> row = decodeIntElement(descriptor, index)
                    6 -> column = decodeIntElement(descriptor, index)
                    7 -> rowSpan = decodeIntElement(descriptor, index)
                    8 -> columnSpan = decodeIntElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> { /* Same as above */ }
                }
            }
        }

        return HomeItem.Widget(
            id = id,
            page = page,
            appWidgetId = appWidgetId,
            packageName = packageName,
            providerClassName = providerClassName,
            row = row,
            column = column,
            rowSpan = rowSpan,
            columnSpan = columnSpan
        )
    }
}

object HomeItemFolderSerializer : KSerializer<HomeItem.Folder> {
    private val appsSerializer = ListSerializer(FolderApp.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HomeItem.Folder") {
        element<String>("id")
        element<Int>("page")
        element<Int>("row")
        element<Int>("column")
        element<Int>("rowSpan")
        element<Int>("columnSpan")
        element<String>("title")
        element<Int>("gridRows")
        element<Int>("gridColumns")
        element("apps", appsSerializer.descriptor)
        element<Float>("appTextSize")
        element<Float>("titleTextSize")
        element<Int>("titleTextColor")
        element<Int>("titleLabelAlignment")
    }

    override fun serialize(encoder: Encoder, value: HomeItem.Folder) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.id)
            encodeIntElement(descriptor, 1, value.page)
            encodeIntElement(descriptor, 2, value.row)
            encodeIntElement(descriptor, 3, value.column)
            encodeIntElement(descriptor, 4, value.rowSpan)
            encodeIntElement(descriptor, 5, value.columnSpan)
            encodeStringElement(descriptor, 6, value.title)
            encodeIntElement(descriptor, 7, value.gridRows)
            encodeIntElement(descriptor, 8, value.gridColumns)
            encodeSerializableElement(descriptor, 9, appsSerializer, value.apps)
            encodeFloatElement(descriptor, 10, value.appTextSize)
            encodeFloatElement(descriptor, 11, value.titleTextSize)
            encodeIntElement(descriptor, 12, value.titleTextColor)
            encodeIntElement(descriptor, 13, value.titleLabelAlignment)
        }
    }

    override fun deserialize(decoder: Decoder): HomeItem.Folder {
        var id = "folder_${System.currentTimeMillis()}"
        var page = 0
        var row = 0
        var column = 0
        var rowSpan = 1
        var columnSpan = 1
        var title = "Folder"
        var gridRows = Constants.GridSize.DEFAULT_ROWS
        var gridColumns = Constants.GridSize.DEFAULT_COLUMNS
        var apps = emptyList<FolderApp>()
        var appTextSize = 1.0f
        var titleTextSize = 1.0f
        var titleTextColor = 0
        var titleLabelAlignment = -1

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeStringElement(descriptor, index)
                    1 -> page = decodeIntElement(descriptor, index)
                    2 -> row = decodeIntElement(descriptor, index)
                    3 -> column = decodeIntElement(descriptor, index)
                    4 -> rowSpan = decodeIntElement(descriptor, index)
                    5 -> columnSpan = decodeIntElement(descriptor, index)
                    6 -> title = decodeStringElement(descriptor, index)
                    7 -> gridRows = decodeIntElement(descriptor, index)
                    8 -> gridColumns = decodeIntElement(descriptor, index)
                    9 -> apps = decodeSerializableElement(descriptor, index, appsSerializer)
                    10 -> appTextSize = decodeFloatElement(descriptor, index)
                    11 -> titleTextSize = decodeFloatElement(descriptor, index)
                    12 -> titleTextColor = decodeIntElement(descriptor, index)
                    13 -> titleLabelAlignment = decodeIntElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> { /* skip unknown fields for forward compat */ }
                }
            }
        }

        return HomeItem.Folder(
            id = id,
            page = page,
            row = row,
            column = column,
            rowSpan = rowSpan,
            columnSpan = columnSpan,
            title = title,
            gridRows = gridRows,
            gridColumns = gridColumns,
            apps = apps,
            appTextSize = appTextSize,
            titleTextSize = titleTextSize,
            titleTextColor = titleTextColor,
            titleLabelAlignment = titleLabelAlignment,
        )
    }
}