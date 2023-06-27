package com.bartaxyz.lensdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.apollographql.apollo3.ApolloClient
import com.bartaxyz.lensdroid.ui.theme.LensdroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import apolloClient
import coil.compose.AsyncImage
import com.apollographql.apollo3.api.Optional
import com.bartaxyz.lens_api.ExplorePublicationsQuery
import com.bartaxyz.lens_api.fragment.PostFields
import com.bartaxyz.lens_api.type.ExplorePublicationRequest
import com.bartaxyz.lens_api.type.PublicationSortCriteria
import com.bartaxyz.lens_api.type.PublicationTypes
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bartaxyz.lensdroid.DateUtils.DateUtils
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.google.accompanist.swiperefresh.SwipeRefresh

class MainActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LensdroidTheme {
                // A surface container using the 'background' color from the theme
                Layout()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Layout() {
    val systemUiController = rememberSystemUiController()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val bottomAppBarColor = NavigationBarDefaults.containerColor

    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Explore", "Artists", "Profile")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Star, Icons.Filled.Person)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Explore Lens",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    actions = {
                        Button(
                            onClick = { /*TODO*/ }) {
                            Text(text = "Connect Wallet")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier.background(bottomAppBarColor)
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(64.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index }
                        )
                    }
                }
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Feed()
            }
        }

        LaunchedEffect(Unit) {
            systemUiController.setSystemBarsColor(
                color = bottomAppBarColor
            )
        }
    }
}

class FeedRepository(private val apolloClient: ApolloClient) {
    suspend fun performGraphQLQuery(nextCursor: String?): ExplorePublicationsQuery.Data {
        val response = withContext(Dispatchers.IO) {
            apolloClient.query(ExplorePublicationsQuery(
                request = ExplorePublicationRequest(
                    sortCriteria = PublicationSortCriteria.TOP_COMMENTED,
                    publicationTypes = Optional.Present(listOf(
                        PublicationTypes.POST,
                        // PublicationTypes.COMMENT,
                        // PublicationTypes.MIRROR
                    )),
                    limit = Optional.Present(24),
                    cursor = Optional.Present(nextCursor)
                )
            )).execute()
        }

        if (response.hasErrors()) {
            // Handle or throw an error
        }

        return response.data ?: throw RuntimeException("Response data is null")
    }
}

class FeedModel(private val repository: FeedRepository) : ViewModel() {
    private val _feedLiveData = MutableLiveData<List<ExplorePublicationsQuery.Item>>()
    val feedLiveData: LiveData<List<ExplorePublicationsQuery.Item>> get() = _feedLiveData

    var noMoreItems = false
    private var nextCursor: String? = null

    init {
        loadMoreItems()
    }

    fun loadMoreItems() {

        if (noMoreItems) return

        viewModelScope.launch {
            val newItems = repository.performGraphQLQuery(nextCursor)
            nextCursor = newItems.explorePublications.pageInfo.next as String?
            noMoreItems = nextCursor == null

            _feedLiveData.value = _feedLiveData.value.orEmpty() + newItems.explorePublications.items
        }
    }
}

class FeedModelFactory(private val repository: FeedRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedModel::class.java)) {
            return FeedModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Feed() {
    val viewModel: FeedModel = viewModel(factory = FeedModelFactory(FeedRepository(apolloClient)))
    val feedData by viewModel.feedLiveData.observeAsState()
    val coroutineScope = rememberCoroutineScope()

    if (feedData == null) {
        return Box(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.progressSemantics().size(20.dp),
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            feedData?.let { items ->
                items(items) { item ->
                    if (item.onPost != null) {
                        Post(item.onPost.postFields)
                    }
                }
            }

            item {
                if (!viewModel.noMoreItems) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.progressSemantics().size(20.dp),
                            strokeWidth = 2.dp,
                            strokeCap = StrokeCap.Round,
                        )
                    }
                }
            }

            coroutineScope.launch {
                if (!viewModel.noMoreItems) {
                    viewModel.loadMoreItems()
                }
            }
        }
    }
}

@Composable
fun Post(post: PostFields) {
    val profile = post.profile.profileFields
    val createdAtFormatted = DateUtils.getRelativeTime(post.createdAt as String)

    val pictureUrl = profile.picture?.onMediaSet?.original?.mediaFields?.url as String?;
    val metadata = post.metadata.metadataOutputFields;
    val content = metadata.content;

    val media = metadata.media.firstOrNull();
    val originalMedia = media?.original?.mediaFields;
    val hasImage = (originalMedia?.mimeType as String?)?.contains("image") == true;
    val firstImageUrl = if (hasImage) originalMedia?.url else null;

    Column {
        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
        Row(modifier = Modifier.padding(12.dp)) {
            Avatar(pictureUrl, modifier = Modifier.padding(top = 6.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    profile.name.let {
                        Text(
                            text = "${profile.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "${profile.handle} • ${createdAtFormatted}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }

                Text(
                    text = "${content ?: "No content"}",
                    fontSize = 15.sp,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (firstImageUrl != null) {
                    Spacer(Modifier.height(16.dp))
                    AsyncImage(
                        model = firstImageUrl,
                        contentDescription = "Post image",
                        // rounded a little
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop

                    )
                }
            }
        }
    }
}

@Composable
fun Avatar (url: String?, modifier: Modifier = Modifier) {
    if (url == null) {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
    }

    AsyncImage(
        model = url,
        contentDescription = "Avatar",
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}