package com.gmail.jorgegilcavazos.ballislife.features.highlights;

import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.repository.highlights.HighlightsRepository;
import com.gmail.jorgegilcavazos.ballislife.features.model.Highlight;
import com.gmail.jorgegilcavazos.ballislife.features.model.HighlightViewType;
import com.gmail.jorgegilcavazos.ballislife.util.Utilities;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class HighlightsPresenter extends BasePresenter<HighlightsView> {

    private static final String TAG = "HighlightsPresenter";

    private HighlightsRepository highlightsRepository;
    private LocalRepository localRepository;
    private BaseSchedulerProvider schedulerProvider;
    private CompositeDisposable disposables;

    @Inject
    public HighlightsPresenter(HighlightsRepository highlightsRepository,
                               LocalRepository localRepository,
                               BaseSchedulerProvider schedulerProvider) {
        this.highlightsRepository = highlightsRepository;
        this.localRepository = localRepository;
        this.schedulerProvider = schedulerProvider;

        disposables = new CompositeDisposable();
    }

    public void setItemsToLoad(int itemsToLoad) {
        highlightsRepository.setItemsToLoad(itemsToLoad);
    }

    public void loadFirstAvailable() {
        List<Highlight> highlights = highlightsRepository.getCachedHighlights();
        if (highlights.isEmpty()) {
            loadHighlights(true);
        } else {
            view.showHighlights(highlights, true);
        }
    }

    public void loadHighlights(final boolean reset) {
        if (reset) {
            view.resetScrollState();
            view.setLoadingIndicator(true);
            highlightsRepository.reset();
        }
        disposables.add(highlightsRepository.next()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableSingleObserver<List<Highlight>>() {
                    @Override
                    public void onSuccess(List<Highlight> highlights) {
                        if (highlights.isEmpty()) {
                            view.showNoHighlightsAvailable();
                        } else {
                            view.showHighlights(highlights, reset);
                        }

                        if (reset) {
                            view.setLoadingIndicator(false);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.showErrorLoadingHighlights();

                        if (reset) {
                            view.setLoadingIndicator(false);
                        }
                    }
                })
        );
    }

    public void subscribeToHighlightsClick(Observable<Highlight> highlightsClick) {
        disposables.add(highlightsClick
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableObserver<Highlight>() {
                    @Override
                    public void onNext(Highlight hl) {
                        if (hl.getUrl().contains("streamable")) {
                            String shortCode = Utilities.getStreamableShortcodeFromUrl(hl.getUrl());
                            if (shortCode != null) {
                                view.openStreamable(shortCode);
                            } else {
                                view.showErrorOpeningStreamable();
                            }
                        } else if (hl.getUrl().contains("youtube") || hl.getUrl().contains("youtu" +
                                ".be")) {
                            String videoId = Utilities.getYoutubeVideoIdFromUrl(hl.getUrl());
                            if (videoId == null) {
                                view.showErrorOpeningYoutube();
                            } else {
                                view.openYoutubeVideo(videoId);
                            }
                        } else {
                            view.showUnknownSourceError();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.showErrorOpeningStreamable();
                    }

                    @Override
                    public void onComplete() {

                    }
                })
        );
    }

    public void subscribeToHighlightsShare(Observable<Highlight> highlightShare) {
        disposables.add(highlightShare
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableObserver<Highlight>() {
                    @Override
                    public void onNext(Highlight highlight) {
                        view.shareHighlight(highlight);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    public void onComplete() {

                    }
                })
        );
    }

    public void onViewTypeSelected(HighlightViewType viewType) {
        localRepository.saveFavoriteHighlightViewType(viewType);
        view.changeViewType(viewType);
    }

    public void stop() {
        if (disposables != null) {
            disposables.clear();
        }
        view.hideSnackbar();
    }

}
