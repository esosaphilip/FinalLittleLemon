package com.example.littlelemon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.littlelemon.ui.theme.LittleLemonTheme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class MainActivity : ComponentActivity() {


    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {

            json(contentType = ContentType("text", "plain"))
        }
    }

    private val database by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database").build()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LittleLemonTheme {
                // add databaseMenuItems code here
                val databaseMenuItems by database.menuItemDao().getAll().observeAsState(emptyList())

                // add orderMenuItems variable here.store information on whether or not sorting is enabled.
                var orderMenuItems by remember { mutableStateOf(false) }
                // add menuItems variable here
                val menuItems = if (orderMenuItems) {
                      databaseMenuItems.sortedBy { it.title }
                } else {
                    databaseMenuItems
                }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "logo",
                        modifier = Modifier.padding(50.dp)
                    )

                    // add Button code here
                     Button(
                         onClick = { orderMenuItems = true },
                         colors = ButtonDefaults.buttonColors(
                             backgroundColor = Color.Yellow,
                             contentColor = contentColorFor(Color.Black),
                         ),
                         elevation = ButtonDefaults.elevation(
                             defaultElevation = 15.dp,
                         ),
                         ) {
                         Text("Tap to Order By Name")
                     }
                    // add searchPhrase variable here

                    var searchMenuItems by remember { mutableStateOf("") }

                    // Add OutlinedTextField
                    OutlinedTextField(
                        value = searchMenuItems,
                        onValueChange = { searchMenuItems = it },
                        label ={ Text("Search") } ,
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 50.dp, end = 50.dp),
                    )

                    // add is not empty check here
                    if(searchMenuItems.isNotEmpty()){
                        menuItems.filter { it.title.contains(searchMenuItems, ignoreCase = true) }
                    }
                    MenuItemsList(items = menuItems )
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {

            if (database.menuItemDao().isEmpty()) {
                // add code here
                val fetchList = fetchMenu()
                saveMenuToDatabase(fetchList)
            }
        }
    }

    private suspend fun fetchMenu(): List<MenuItemNetwork> {
        // data URL: https://raw.githubusercontent.com/Meta-Mobile-Developer-PC/Working-With-Data-API/main/littleLemonSimpleMenu.json
        val response = httpClient
            .get("https://raw.githubusercontent.com/Meta-Mobile-Developer-PC/Working-With-Data-API/main/littleLemonSimpleMenu.json")
            .body<String>()

        return  json.decodeFromString<List<MenuItemNetwork>>(response)
    }

    private fun saveMenuToDatabase(menuItemsNetwork: List<MenuItemNetwork>) {
        val menuItemsRoom = menuItemsNetwork.map { it.toMenuItemRoom() }
        database.menuItemDao().insertAll(*menuItemsRoom.toTypedArray())
    }
}

@Composable
private fun MenuItemsList(items: List<MenuItemRoom>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 20.dp)
    ) {
        items(
            items = items,
            itemContent = { menuItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(menuItem.title)
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                        textAlign = TextAlign.Right,
                        text = "%.2f".format(menuItem.price)
                    )
                }
            }
        )
    }
}
