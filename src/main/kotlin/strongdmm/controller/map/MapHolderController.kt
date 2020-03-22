package strongdmm.controller.map

import gnu.trove.map.hash.TIntObjectHashMap
import io.github.spair.dmm.io.reader.DmmReader
import strongdmm.byond.dme.Dme
import strongdmm.byond.dmm.Dmm
import strongdmm.byond.dmm.save.SaveMap
import strongdmm.event.Event
import strongdmm.event.EventConsumer
import strongdmm.event.EventSender
import strongdmm.event.MapId
import strongdmm.event.type.EventGlobal
import strongdmm.event.type.EventGlobalProvider
import strongdmm.event.type.controller.EventEnvironmentController
import strongdmm.event.type.controller.EventMapHolderController
import java.io.File
import java.nio.file.Files
import kotlin.concurrent.thread

class MapHolderController : EventSender, EventConsumer {
    private val mapsBackupPathsById: TIntObjectHashMap<String> = TIntObjectHashMap()
    private val openedMaps: MutableSet<Dmm> = mutableSetOf()
    private val availableMapsPathsWithVisibleMapsPaths: MutableSet<Pair<String, String>> = mutableSetOf()

    private var selectedMap: Dmm? = null

    init {
        consumeEvent(EventMapHolderController.Open::class.java, ::handleOpen)
        consumeEvent(EventMapHolderController.Close::class.java, ::handleClose)
        consumeEvent(EventMapHolderController.FetchSelected::class.java, ::handleFetchSelected)
        consumeEvent(EventMapHolderController.Change::class.java, ::handleChange)
        consumeEvent(EventMapHolderController.Save::class.java, ::handleSave)
        consumeEvent(EventGlobal.EnvironmentReset::class.java, ::handleEnvironmentReset)
        consumeEvent(EventGlobal.EnvironmentChanged::class.java, ::handleEnvironmentChanged)
    }

    fun postInit() {
        sendEvent(EventGlobalProvider.OpenedMaps(openedMaps))
        sendEvent(EventGlobalProvider.AvailableMaps(availableMapsPathsWithVisibleMapsPaths))
    }

    private fun handleOpen(event: Event<File, Unit>) {
        val id = event.body.absolutePath.hashCode()

        if (selectedMap?.id == id) {
            return
        }

        val dmm = openedMaps.find { it.id == id }

        if (dmm != null) {
            selectedMap = dmm
            sendEvent(EventGlobal.SelectedMapChanged(dmm))
        } else {
            val mapFile = event.body

            if (!mapFile.isFile) {
                return
            }

            sendEvent(EventEnvironmentController.Fetch { environment ->
                val dmmData = DmmReader.readMap(mapFile)
                val map = Dmm(mapFile, dmmData, environment)

                val tmpDmmDataFile = Files.createTempFile("sdmm-", ".dmm.backup").toFile()
                tmpDmmDataFile.writeBytes(mapFile.readBytes())
                mapsBackupPathsById.put(id, tmpDmmDataFile.absolutePath)
                tmpDmmDataFile.deleteOnExit()

                openedMaps.add(map)
                selectedMap = map

                sendEvent(EventGlobal.SelectedMapChanged(map))
            })
        }
    }

    private fun handleClose(event: Event<MapId, Unit>) {
        openedMaps.find { it.id == event.body }?.let {
            val mapIndex = openedMaps.indexOf(it)

            mapsBackupPathsById.remove(it.id)
            openedMaps.remove(it)
            sendEvent(EventGlobal.OpenedMapClosed(it))

            if (selectedMap === it) {
                if (openedMaps.isEmpty()) {
                    selectedMap = null
                } else {
                    val index = if (mapIndex == openedMaps.size) mapIndex - 1 else mapIndex
                    val nextMap = openedMaps.toList()[index]
                    selectedMap = nextMap
                    sendEvent(EventGlobal.SelectedMapChanged(nextMap))
                }
            }
        }
    }

    private fun handleFetchSelected(event: Event<Unit, Dmm>) {
        selectedMap?.let { event.reply(it) }
    }

    private fun handleChange(event: Event<MapId, Unit>) {
        openedMaps.find { it.id == event.body }?.let {
            if (selectedMap !== it) {
                selectedMap = it
                sendEvent(EventGlobal.SelectedMapChanged(it))
            }
        }
    }

    private fun handleSave() {
        selectedMap?.let { map ->
            thread(start = true) {
                val initialDmmData = DmmReader.readMap(File(mapsBackupPathsById.get(map.id)))
                SaveMap(map, initialDmmData, true)
            }
        }
    }

    private fun handleEnvironmentReset() {
        selectedMap = null
        openedMaps.clear()
        availableMapsPathsWithVisibleMapsPaths.clear()
    }

    private fun handleEnvironmentChanged(event: Event<Dme, Unit>) {
        File(event.body.rootPath).walkTopDown().forEach {
            if (it.extension == "dmm") {
                val absoluteFilePath = it.absolutePath
                val visibleName = File(event.body.rootPath).toPath().relativize(it.toPath()).toString()
                availableMapsPathsWithVisibleMapsPaths.add(absoluteFilePath to visibleName)
            }
        }
    }
}
