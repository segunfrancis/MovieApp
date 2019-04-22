package com.example.computer.moviesearch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.computer.moviesearch.adapter.TrailerAdapter;
import com.example.computer.moviesearch.api.Client;
import com.example.computer.moviesearch.api.Service;
import com.example.computer.moviesearch.data.FavoriteDbHelper;
import com.example.computer.moviesearch.model.Movie;
import com.example.computer.moviesearch.model.Trailer;
import com.example.computer.moviesearch.model.TrailerResponse;
import com.github.ivbaranov.mfb.MaterialFavoriteButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {

    TextView nameOfMovie, plotSynopsis, userRating, releaseDate;
    ImageView imageView;
    private RecyclerView recyclerView;
    private TrailerAdapter adapter;
    private List<Trailer> trailerList;
    private FavoriteDbHelper favoriteDbHelper;
    private Movie favorite;
    private final AppCompatActivity activity = DetailActivity.this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            initCollapsingToolbar();
        } else {
            initCollapsingToolbar();
        }

        imageView = findViewById(R.id.thumbnail_image_header);
        nameOfMovie = findViewById(R.id.title);
        plotSynopsis = findViewById(R.id.plot_synopsis);
        userRating = findViewById(R.id.user_rating);
        releaseDate = findViewById(R.id.release_date);

        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra("title")) {
            String thumbnail = getIntent().getExtras().getString("poster_path");
            String movieName = getIntent().getExtras().getString("title");
            String synopsis = getIntent().getExtras().getString("overview");
            String rating = getIntent().getExtras().getString("vote_average");
            String dateOfRelease = getIntent().getExtras().getString("release_date");

            // Use glide to set up image
            RequestOptions options = new RequestOptions().fitCenter().placeholder(R.drawable.loading)
                    .error(R.drawable.error_image);
            Glide.with(this).load(thumbnail).apply(options).into(imageView);

            nameOfMovie.setText(movieName);
            plotSynopsis.setText(synopsis);
            userRating.setText(rating);
            releaseDate.setText(dateOfRelease);
        } else {
            Toast.makeText(this, "No API Data!", Toast.LENGTH_SHORT).show();
        }

        final MaterialFavoriteButton materialFavoriteButtonNice = findViewById(R.id.favorite_button);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        materialFavoriteButtonNice.setOnFavoriteChangeListener(new MaterialFavoriteButton.OnFavoriteChangeListener() {
            @Override
            public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                if (favorite) {
                    SharedPreferences.Editor editor = getSharedPreferences("com.example.computer.moviesearch.DetailActivity", MODE_PRIVATE).edit();
                    editor.putBoolean("Favorite Added", true);
                    editor.apply();
                    saveFavorite();
                    Snackbar.make(buttonView, "Added to Favorite", Snackbar.LENGTH_SHORT).show();
                } else {
                    int movie_id = getIntent().getExtras().getInt("id");
                    favoriteDbHelper = new FavoriteDbHelper(activity);
                    favoriteDbHelper.deleteFavorite(movie_id);

                    SharedPreferences.Editor editor = getSharedPreferences("com.example.computer.moviesearch.DetailActivity", MODE_PRIVATE).edit();
                    editor.putBoolean("Favorite Removed", true);
                    editor.apply();
                    Snackbar.make(buttonView, "Removed from Favorite", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        initViews();
    }

    private void initCollapsingToolbar() {
        final CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(" ");
        AppBarLayout appBarLayout = findViewById(R.id.appBar);
        appBarLayout.setExpanded(true);

        // Hide/show title when toolbar expands and collapse
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShown = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    /*TODO: change for scrolling effect*/
                    collapsingToolbarLayout.setTitle(getIntent().getStringExtra("title"));
                    isShown = true;
                } else if (isShown) {
                    collapsingToolbarLayout.setTitle(" ");
                    isShown = false;
                }
            }
        });
    }

    private void initViews() {
        trailerList = new ArrayList<>();
        adapter = new TrailerAdapter(this, trailerList);

        recyclerView = findViewById(R.id.recycler_view1);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        loadJSON();
    }

    private void loadJSON() {
        int movie_id = getIntent().getExtras().getInt("id");
        try {
            if (BuildConfig.THE_MOVIE_DB_API_TOKEN.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please obtain your API key from themoviedb.org", Toast.LENGTH_SHORT).show();
                return;
            }
            Client Client = new Client();
            Service apiService = Client.getClient().create(Service.class);
            Call<TrailerResponse> call = apiService.getMovieTrailer(movie_id, BuildConfig.THE_MOVIE_DB_API_TOKEN);
            call.enqueue(new Callback<TrailerResponse>() {
                @Override
                public void onResponse(Call<TrailerResponse> call, Response<TrailerResponse> response) {
                    List<Trailer> trailer = null;
                    if (response.body() != null) {
                        trailer = response.body().getResults();

                    recyclerView.setAdapter(new TrailerAdapter(DetailActivity.this, trailer));
                    recyclerView.smoothScrollToPosition(0);
                    } else return;
                }

                @Override
                public void onFailure(Call<TrailerResponse> call, Throwable t) {
                    Log.d("Error", t.getMessage());
                    Toast.makeText(DetailActivity.this, "Error fetching trailer data", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFavorite() {
        favoriteDbHelper = new FavoriteDbHelper(activity);
        favorite = new Movie();
        int movie_id = getIntent().getExtras().getInt("id");
        String rate = getIntent().getExtras().getString("vote_average");
        String poster = getIntent().getExtras().getString("poster_path");

        favorite.setId(movie_id);
        favorite.setTitle(nameOfMovie.getText().toString().trim());
        favorite.setPosterPath(poster);
        favorite.setVoteAverage(Double.parseDouble(rate));
        favorite.setOverview(plotSynopsis.getText().toString().trim());

        favoriteDbHelper.addFavorite(favorite);
    }
}
