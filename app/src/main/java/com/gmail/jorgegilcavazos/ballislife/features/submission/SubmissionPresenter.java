package com.gmail.jorgegilcavazos.ballislife.features.submission;

import android.content.SharedPreferences;

import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter;
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthenticationImpl;
import com.gmail.jorgegilcavazos.ballislife.data.service.RedditServiceImpl;
import com.gmail.jorgegilcavazos.ballislife.features.model.wrapper.CustomSubmission;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.Utilities;
import com.gmail.jorgegilcavazos.ballislife.util.exception.NotLoggedInException;
import com.gmail.jorgegilcavazos.ballislife.util.exception.ReplyNotAvailableException;
import com.gmail.jorgegilcavazos.ballislife.util.exception.ReplyToCommentException;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.SingleSource;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class SubmissionPresenter extends BasePresenter<SubmissionView> {

    private RedditServiceImpl redditService;
    private SharedPreferences preferences;
    private CompositeDisposable disposables;
    private BaseSchedulerProvider schedulerProvider;

    public SubmissionPresenter(RedditServiceImpl redditService,
                               SharedPreferences preferences,
                               BaseSchedulerProvider schedulerProvider) {
        this.redditService = redditService;
        this.preferences = preferences;
        this.schedulerProvider = schedulerProvider;

        disposables = new CompositeDisposable();
    }

    public void loadComments(String threadId, CommentSort sorting) {
        view.hideFab();
        view.setLoadingIndicator(true);
        disposables.add(RedditAuthenticationImpl.getInstance().authenticate(preferences)
                .andThen(redditService.getSubmission(threadId, sorting))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableSingleObserver<Submission>() {
                    @Override
                    public void onSuccess(Submission submission) {
                        Iterable<CommentNode> iterable = submission.getComments().walkTree();
                        List<CommentNode> commentNodes = new ArrayList<>();
                        for (CommentNode node : iterable) {
                            commentNodes.add(node);
                        }

                        view.setCustomSubmission(new CustomSubmission(submission));
                        view.showComments(commentNodes, submission);
                        view.setLoadingIndicator(false);
                        view.scrollToTop();
                        view.showFab();
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.setLoadingIndicator(false);
                    }
                })
        );
    }

    public void onVoteSubmission(Submission submission, VoteDirection vote) {
        if (!RedditAuthenticationImpl.getInstance().isUserLoggedIn()) {
            view.showNotLoggedInError();
            return;
        }

        disposables.add(RedditAuthenticationImpl.getInstance().authenticate(preferences)
                .andThen(redditService.voteSubmission(submission, vote))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                })
        );
    }

    public void onSaveSubmission(Submission submission, boolean saved) {
        if (!RedditAuthenticationImpl.getInstance().isUserLoggedIn()) {
            view.showNotLoggedInError();
            return;
        }

        disposables.add(RedditAuthenticationImpl.getInstance().authenticate(preferences)
                .andThen(redditService.saveSubmission(submission, saved))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }
                })
        );
    }

    public void onVoteComment(Comment comment, VoteDirection vote) {
        if (!RedditAuthenticationImpl.getInstance().isUserLoggedIn()) {
            view.showNotLoggedInError();
            return;
        }

        disposables.add(RedditAuthenticationImpl.getInstance().authenticate(preferences)
                .andThen(redditService.voteComment(comment, vote))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                })
        );
    }

    public void onSaveComment(Comment comment) {
        if (!RedditAuthenticationImpl.getInstance().isUserLoggedIn()) {
            view.showNotLoggedInError();
            return;
        }

        disposables.add(RedditAuthenticationImpl.getInstance().authenticate(preferences)
                .andThen(redditService.saveComment(comment))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }
                })
        );
    }

    public void onUnsaveComment(Comment comment) {
        if (!RedditAuthenticationImpl.getInstance().isUserLoggedIn()) {
            view.showNotLoggedInError();
            return;
        }

        disposables.add(RedditAuthenticationImpl.getInstance().authenticate(preferences)
                .andThen(redditService.unsaveComment(comment))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }
                })
        );
    }

    public void onReplyToCommentBtnClick(int position, Comment parent) {
        if (!RedditAuthenticationImpl.getInstance().isUserLoggedIn()) {
            view.showNotLoggedInError();
            return;
        }

        view.openReplyToCommentDialog(position, parent);
    }

    public void onReplyToComment(final int position, final Comment parent, String text) {
        view.showSavingToast();
        disposables.add(RedditAuthenticationImpl.getInstance().authenticate(preferences)
                .andThen(redditService.replyToComment(parent, text))
                // Comment is not immediately available after being posted in the next call
                // (probably a small delay from reddit's servers) so we need to wait for a bit
                // before fetching the posted comment.
                .delay(3, TimeUnit.SECONDS)
                .flatMap(new Function<String, SingleSource<CommentNode>>() {
                    @Override
                    public SingleSource<CommentNode> apply(String s) throws Exception {
                        return redditService.getComment(parent.getSubmissionId().substring(3), s);
                    }
                })
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io())
                .subscribeWith(new DisposableSingleObserver<CommentNode>() {
                    @Override
                    public void onSuccess(CommentNode comment) {
                        view.addComment(comment, position + 1);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof NotLoggedInException) {
                            view.showNotLoggedInError();
                        } else if (e instanceof ReplyToCommentException) {
                            view.showErrorAddingComment();
                        } else if (e instanceof ReplyNotAvailableException) {
                            // Reply was posted but could not be fetched to display in the UI.
                            view.showSavedToast();
                        } else {
                            view.showErrorSavingToast();
                        }
                    }
                })
        );
    }

    public void onReplyToThreadBtnClick() {
        if (!RedditAuthenticationImpl.getInstance().isUserLoggedIn()) {
            view.showNotLoggedInError();
            return;
        }

        view.openReplyToSubmissionDialog();
    }

    public void onReplyToThread(String text, final Submission submission) {
        view.showSavingToast();
        disposables.add(RedditAuthenticationImpl.getInstance().authenticate(preferences)
                .andThen(redditService.replyToThread(submission, text))
                // Comment is not immediately available after being posted in the next call
                // (probably a small delay from reddit's servers) so we need to wait for a bit
                // before fetching the posted comment.
                .delay(3, TimeUnit.SECONDS)
                .flatMap(new Function<String, SingleSource<CommentNode>>() {
                    @Override
                    public SingleSource<CommentNode> apply(String commentId) throws Exception {
                        return redditService.getComment(submission.getId(), commentId);
                    }
                })
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableSingleObserver<CommentNode>() {
                    @Override
                    public void onSuccess(CommentNode comment) {
                        view.addComment(comment, 0);
                        view.scrollToTop();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof NotLoggedInException) {
                            view.showNotLoggedInError();
                        } else if (e instanceof ReplyToCommentException) {
                            view.showErrorAddingComment();
                        } else if (e instanceof ReplyNotAvailableException) {
                            // Reply was posted but could not be fetched to display in the UI.
                            view.showSavedToast();
                        } else {
                            view.showErrorSavingToast();
                        }
                    }
                })
        );
    }

    public void onContentClick(final String url) {
        if (url != null) {
            if (url.contains(Constants.STREAMABLE_DOMAIN)) {
                String shortCode = Utilities.getStreamableShortcodeFromUrl(url);
                if (shortCode != null) {
                    view.openStreamable(shortCode);
                } else {
                    view.openContentTab(url);
                }
            } else {
                view.openContentTab(url);
            }
        } else {
            view.showContentUnavailableToast();
        }
    }

    public void stop() {
        if (disposables != null) {
            disposables.clear();
        }
    }
}
