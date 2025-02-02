/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2;

import static com.google.android.exoplayer2.util.Assertions.checkState;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Pair;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.source.ads.AdPlaybackState;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

/**
 * A flexible representation of the structure of media. A timeline is able to represent the
 * structure of a wide variety of media, from simple cases like a single media file through to
 * complex compositions of media such as playlists and streams with inserted ads. Instances are
 * immutable. For cases where media is changing dynamically (e.g. live streams), a timeline provides
 * a snapshot of the current state.
 *
 * 媒体结构的灵活表示。时间线能够代表各种媒体的结构，从简单的情况（例如单个媒体文件）到复杂的媒体组成（例如播放列表和带有插入广告的流）。
 * 实例是不可变的。对于媒体动态变化的情况（例如，实时流），时间线可提供当前状态的快照。
 * 时间线由Windows和句点组成。
 * 一个Timeline.Window通常对应一个播放列表项。它可能跨越一个或多个时间段，并定义了当前可用于播放的那些时间段内的区域。
 * 窗口还提供其他信息，例如，窗口内是否支持搜索以及默认位置，默认位置是播放器开始播放窗口时从其开始播放的位置。
 * Timeline.Period定义单个逻辑媒体，例如媒体文件。它还可以定义插入媒体的广告组，以及有关这些广告是否已加载和播放的信息。
 * 以下示例说明了各种用例的时间表。
 * 单个媒体文件或点播流
 *
 * 单个媒体文件或点播流的时间轴由单个时段和窗口组成。该窗口跨越整个周期，指示媒体的所有部分都可以播放。窗口的默认位置通常在时间段的开始（由上图中的黑点表示）。
 * 媒体文件或点播流的播放列表
 *
 * 媒体文件或点播流播放列表的时间轴包含多个时段，每个时段都有其自己的窗口。每个窗口跨越整个相应的期间，通常在该期间的开始处具有默认位置。周期和窗口的属性（例如它们的持续时间以及窗口是否可搜索）通常仅在播放器开始缓冲相应的文件或流时才知道。
 * 直播能力有限
 *
 * 实时流的时间轴由一个持续时间未知的时段组成，因为随着广播更多内容，该持续时间会不断延长。如果内容仅在有限的时间段内保持可用，则窗口可能会在非零位置开始，从而定义仍可以播放的内容区域。该窗口将从Timeline.Window.isLive（）返回true，以指示它是实时流，并且只要我们期望对实时窗口进行更改，Timeline.Window.isDynamic将设置为true。它的默认位置通常靠近活动边缘（由上图中的黑点表示）。
 * 具有无限可用性的实时流
 *
 * 具有无限可用性的实时流的时间线与具有有限可用性的实时流的时间线相似，不同之处在于窗口从时段开始时开始，以指示仍可以播放所有先前广播的内容。
 * 多个时段的即时串流
 *
 * 当实况流明确地划分为单独的时间段时（例如在内容边界处），就会出现这种情况。这种情况类似于“具有有限可用性的实时流”情况，不同之处在于窗口可能跨越一个以上的时间段。在无限可用性的情况下，也可能有多个期间。
 * 点播流和直播流
 *
 * 这种情况是“单个”媒体文件或点播流和带有多个句点的实时流的串联。当点播流的回放结束时，实时流的回放将从其在实时边缘附近的默认位置开始。
 * 带有插播广告的点播流
 *
 * 这种情况包括插播广告组，它们被定义为时间轴的单个时间段的一部分。可以查询该期间以获取有关广告组及其包含的广告的信息。
 *   ExoPlayer.library-common
 *
 * <p>A timeline consists of {@link Window Windows} and {@link Period Periods}.
 *
 * <ul>
 *   <li>A {@link Window} usually corresponds to one playlist item. It may span one or more periods
 *       and it defines the region within those periods that's currently available for playback. The
 *       window also provides additional information such as whether seeking is supported within the
 *       window and the default position, which is the position from which playback will start when
 *       the player starts playing the window.
 *   <li>A {@link Period} defines a single logical piece of media, for example a media file. It may
 *       also define groups of ads inserted into the media, along with information about whether
 *       those ads have been loaded and played.
 * </ul>
 *
 * <p>The following examples illustrate timelines for various use cases.
 *
 * <h3 id="single-file">Single media file or on-demand stream</h3>
 *
 * <p style="align:center"><img src="doc-files/timeline-single-file.svg" alt="Example timeline for a
 * single file">
 *
 * <p>A timeline for a single media file or on-demand stream consists of a single period and window.
 * The window spans the whole period, indicating that all parts of the media are available for
 * playback. The window's default position is typically at the start of the period (indicated by the
 * black dot in the figure above).
 *
 * <h3>Playlist of media files or on-demand streams</h3>
 *
 * <p style="align:center"><img src="doc-files/timeline-playlist.svg" alt="Example timeline for a
 * playlist of files">
 *
 * <p>A timeline for a playlist of media files or on-demand streams consists of multiple periods,
 * each with its own window. Each window spans the whole of the corresponding period, and typically
 * has a default position at the start of the period. The properties of the periods and windows
 * (e.g. their durations and whether the window is seekable) will often only become known when the
 * player starts buffering the corresponding file or stream.
 *
 * <h3 id="live-limited">Live stream with limited availability</h3>
 *
 * <p style="align:center"><img src="doc-files/timeline-live-limited.svg" alt="Example timeline for
 * a live stream with limited availability">
 *
 * <p>A timeline for a live stream consists of a period whose duration is unknown, since it's
 * continually extending as more content is broadcast. If content only remains available for a
 * limited period of time then the window may start at a non-zero position, defining the region of
 * content that can still be played. The window will return true from {@link Window#isLive()} to
 * indicate it's a live stream and {@link Window#isDynamic} will be set to true as long as we expect
 * changes to the live window. Its default position is typically near to the live edge (indicated by
 * the black dot in the figure above).
 *
 * <h3>Live stream with indefinite availability</h3>
 *
 * <p style="align:center"><img src="doc-files/timeline-live-indefinite.svg" alt="Example timeline
 * for a live stream with indefinite availability">
 *
 * <p>A timeline for a live stream with indefinite availability is similar to the <a
 * href="#live-limited">Live stream with limited availability</a> case, except that the window
 * starts at the beginning of the period to indicate that all of the previously broadcast content
 * can still be played.
 *
 * <h3 id="live-multi-period">Live stream with multiple periods</h3>
 *
 * <p style="align:center"><img src="doc-files/timeline-live-multi-period.svg" alt="Example timeline
 * for a live stream with multiple periods">
 *
 * <p>This case arises when a live stream is explicitly divided into separate periods, for example
 * at content boundaries. This case is similar to the <a href="#live-limited">Live stream with
 * limited availability</a> case, except that the window may span more than one period. Multiple
 * periods are also possible in the indefinite availability case.
 *
 * <h3>On-demand stream followed by live stream</h3>
 *
 * <p style="align:center"><img src="doc-files/timeline-advanced.svg" alt="Example timeline for an
 * on-demand stream followed by a live stream">
 *
 * <p>This case is the concatenation of the <a href="#single-file">Single media file or on-demand
 * stream</a> and <a href="#multi-period">Live stream with multiple periods</a> cases. When playback
 * of the on-demand stream ends, playback of the live stream will start from its default position
 * near the live edge.
 *
 * <h3 id="single-file-midrolls">On-demand stream with mid-roll ads</h3>
 *
 * <p style="align:center"><img src="doc-files/timeline-single-file-midrolls.svg" alt="Example
 * timeline for an on-demand stream with mid-roll ad groups">
 *
 * <p>This case includes mid-roll ad groups, which are defined as part of the timeline's single
 * period. The period can be queried for information about the ad groups and the ads they contain.
 */
public abstract class Timeline {

  /**
   * Holds information about a window in a {@link Timeline}. A window usually corresponds to one
   * playlist item and defines a region of media currently available for playback along with
   * additional information such as whether seeking is supported within the window. The figure below
   * shows some of the information defined by a window, as well as how this information relates to
   * corresponding {@link Period Periods} in the timeline.
   *
   * <p style="align:center"><img src="doc-files/timeline-window.svg" alt="Information defined by a
   * timeline window">
   */
  public static final class Window {

    /**
     * A {@link #uid} for a window that must be used for single-window {@link Timeline Timelines}.
     */
    public static final Object SINGLE_WINDOW_UID = new Object();

    private static final MediaItem EMPTY_MEDIA_ITEM =
        new MediaItem.Builder()
            .setMediaId("com.google.android.exoplayer2.Timeline")
            .setUri(Uri.EMPTY)
            .build();

    /**
     * A unique identifier for the window. Single-window {@link Timeline Timelines} must use {@link
     * #SINGLE_WINDOW_UID}.
     */
    public Object uid;

    /** @deprecated Use {@link #mediaItem} instead. */
    @Deprecated @Nullable public Object tag;

    /** The {@link MediaItem} associated to the window. Not necessarily unique. */
    public MediaItem mediaItem;

    /** The manifest of the window. May be {@code null}. */
    @Nullable public Object manifest;

    /**
     * The start time of the presentation to which this window belongs in milliseconds since the
     * Unix epoch, or {@link C#TIME_UNSET} if unknown or not applicable. For informational purposes
     * only.
     */
    public long presentationStartTimeMs;

    /**
     * The window's start time in milliseconds since the Unix epoch, or {@link C#TIME_UNSET} if
     * unknown or not applicable.
     */
    public long windowStartTimeMs;

    /**
     * The offset between {@link SystemClock#elapsedRealtime()} and the time since the Unix epoch
     * according to the clock of the media origin server, or {@link C#TIME_UNSET} if unknown or not
     * applicable.
     *
     * <p>Note that the current Unix time can be retrieved using {@link #getCurrentUnixTimeMs()} and
     * is calculated as {@code SystemClock.elapsedRealtime() + elapsedRealtimeEpochOffsetMs}.
     */
    public long elapsedRealtimeEpochOffsetMs;

    /** Whether it's possible to seek within this window. */
    public boolean isSeekable;

    // TODO: Split this to better describe which parts of the window might change. For example it
    // should be possible to individually determine whether the start and end positions of the
    // window may change relative to the underlying periods. For an example of where it's useful to
    // know that the end position is fixed whilst the start position may still change, see:
    // https://github.com/google/ExoPlayer/issues/4780.
    /** Whether this window may change when the timeline is updated. */
    public boolean isDynamic;

    /** @deprecated Use {@link #isLive()} instead. */
    @Deprecated public boolean isLive;

    /**
     * The {@link MediaItem.LiveConfiguration} that is used or null if {@link #isLive()} returns
     * false.
     */
    @Nullable public MediaItem.LiveConfiguration liveConfiguration;

    /**
     * Whether this window contains placeholder information because the real information has yet to
     * be loaded.
     */
    public boolean isPlaceholder;

    /** The index of the first period that belongs to this window. */
    public int firstPeriodIndex;

    /**
     * The index of the last period that belongs to this window.
     */
    public int lastPeriodIndex;

    /**
     * The default position relative to the start of the window at which to begin playback, in
     * microseconds. May be {@link C#TIME_UNSET} if and only if the window was populated with a
     * non-zero default position projection, and if the specified projection cannot be performed
     * whilst remaining within the bounds of the window.
     */
    public long defaultPositionUs;

    /**
     * The duration of this window in microseconds, or {@link C#TIME_UNSET} if unknown.
     */
    public long durationUs;

    /**
     * The position of the start of this window relative to the start of the first period belonging
     * to it, in microseconds.
     */
    public long positionInFirstPeriodUs;

    /** Creates window. */
    public Window() {
      uid = SINGLE_WINDOW_UID;
      mediaItem = EMPTY_MEDIA_ITEM;
    }

    /** Sets the data held by this window. */
    @SuppressWarnings("deprecation")
    public Window set(
        Object uid,
        @Nullable MediaItem mediaItem,
        @Nullable Object manifest,
        long presentationStartTimeMs,
        long windowStartTimeMs,
        long elapsedRealtimeEpochOffsetMs,
        boolean isSeekable,
        boolean isDynamic,
        @Nullable MediaItem.LiveConfiguration liveConfiguration,
        long defaultPositionUs,
        long durationUs,
        int firstPeriodIndex,
        int lastPeriodIndex,
        long positionInFirstPeriodUs) {
      this.uid = uid;
      this.mediaItem = mediaItem != null ? mediaItem : EMPTY_MEDIA_ITEM;
      this.tag =
          mediaItem != null && mediaItem.playbackProperties != null
              ? mediaItem.playbackProperties.tag
              : null;
      this.manifest = manifest;
      this.presentationStartTimeMs = presentationStartTimeMs;
      this.windowStartTimeMs = windowStartTimeMs;
      this.elapsedRealtimeEpochOffsetMs = elapsedRealtimeEpochOffsetMs;
      this.isSeekable = isSeekable;
      this.isDynamic = isDynamic;
      this.isLive = liveConfiguration != null;
      this.liveConfiguration = liveConfiguration;
      this.defaultPositionUs = defaultPositionUs;
      this.durationUs = durationUs;
      this.firstPeriodIndex = firstPeriodIndex;
      this.lastPeriodIndex = lastPeriodIndex;
      this.positionInFirstPeriodUs = positionInFirstPeriodUs;
      this.isPlaceholder = false;
      return this;
    }

    /**
     * Returns the default position relative to the start of the window at which to begin playback,
     * in milliseconds. May be {@link C#TIME_UNSET} if and only if the window was populated with a
     * non-zero default position projection, and if the specified projection cannot be performed
     * whilst remaining within the bounds of the window.
     */
    public long getDefaultPositionMs() {
      return C.usToMs(defaultPositionUs);
    }

    /**
     * Returns the default position relative to the start of the window at which to begin playback,
     * in microseconds. May be {@link C#TIME_UNSET} if and only if the window was populated with a
     * non-zero default position projection, and if the specified projection cannot be performed
     * whilst remaining within the bounds of the window.
     */
    public long getDefaultPositionUs() {
      return defaultPositionUs;
    }

    /**
     * Returns the duration of the window in milliseconds, or {@link C#TIME_UNSET} if unknown.
     */
    public long getDurationMs() {
      return C.usToMs(durationUs);
    }

    /**
     * Returns the duration of this window in microseconds, or {@link C#TIME_UNSET} if unknown.
     */
    public long getDurationUs() {
      return durationUs;
    }

    /**
     * Returns the position of the start of this window relative to the start of the first period
     * belonging to it, in milliseconds.
     */
    public long getPositionInFirstPeriodMs() {
      return C.usToMs(positionInFirstPeriodUs);
    }

    /**
     * Returns the position of the start of this window relative to the start of the first period
     * belonging to it, in microseconds.
     */
    public long getPositionInFirstPeriodUs() {
      return positionInFirstPeriodUs;
    }

    /**
     * Returns the current time in milliseconds since the Unix epoch.
     *
     * <p>This method applies {@link #elapsedRealtimeEpochOffsetMs known corrections} made available
     * by the media such that this time corresponds to the clock of the media origin server.
     */
    public long getCurrentUnixTimeMs() {
      return Util.getNowUnixTimeMs(elapsedRealtimeEpochOffsetMs);
    }

    /** Returns whether this is a live stream. */
    // Verifies whether the deprecated isLive member field is in a correct state.
    @SuppressWarnings("deprecation")
    public boolean isLive() {
      checkState(isLive == (liveConfiguration != null));
      return liveConfiguration != null;
    }

    // Provide backward compatibility for tag.
    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !getClass().equals(obj.getClass())) {
        return false;
      }
      Window that = (Window) obj;
      return Util.areEqual(uid, that.uid)
          && Util.areEqual(mediaItem, that.mediaItem)
          && Util.areEqual(manifest, that.manifest)
          && Util.areEqual(liveConfiguration, that.liveConfiguration)
          && presentationStartTimeMs == that.presentationStartTimeMs
          && windowStartTimeMs == that.windowStartTimeMs
          && elapsedRealtimeEpochOffsetMs == that.elapsedRealtimeEpochOffsetMs
          && isSeekable == that.isSeekable
          && isDynamic == that.isDynamic
          && isPlaceholder == that.isPlaceholder
          && defaultPositionUs == that.defaultPositionUs
          && durationUs == that.durationUs
          && firstPeriodIndex == that.firstPeriodIndex
          && lastPeriodIndex == that.lastPeriodIndex
          && positionInFirstPeriodUs == that.positionInFirstPeriodUs;
    }

    // Provide backward compatibility for tag.
    @Override
    public int hashCode() {
      int result = 7;
      result = 31 * result + uid.hashCode();
      result = 31 * result + mediaItem.hashCode();
      result = 31 * result + (manifest == null ? 0 : manifest.hashCode());
      result = 31 * result + (liveConfiguration == null ? 0 : liveConfiguration.hashCode());
      result = 31 * result + (int) (presentationStartTimeMs ^ (presentationStartTimeMs >>> 32));
      result = 31 * result + (int) (windowStartTimeMs ^ (windowStartTimeMs >>> 32));
      result =
          31 * result
              + (int) (elapsedRealtimeEpochOffsetMs ^ (elapsedRealtimeEpochOffsetMs >>> 32));
      result = 31 * result + (isSeekable ? 1 : 0);
      result = 31 * result + (isDynamic ? 1 : 0);
      result = 31 * result + (isPlaceholder ? 1 : 0);
      result = 31 * result + (int) (defaultPositionUs ^ (defaultPositionUs >>> 32));
      result = 31 * result + (int) (durationUs ^ (durationUs >>> 32));
      result = 31 * result + firstPeriodIndex;
      result = 31 * result + lastPeriodIndex;
      result = 31 * result + (int) (positionInFirstPeriodUs ^ (positionInFirstPeriodUs >>> 32));
      return result;
    }
  }

  /**
   * Holds information about a period in a {@link Timeline}. A period defines a single logical piece
   * of media, for example a media file. It may also define groups of ads inserted into the media,
   * along with information about whether those ads have been loaded and played.
   *
   * <p>The figure below shows some of the information defined by a period, as well as how this
   * information relates to a corresponding {@link Window} in the timeline.
   *
   * <p style="align:center"><img src="doc-files/timeline-period.svg" alt="Information defined by a
   * period">
   */
  public static final class Period {

    /**
     * An identifier for the period. Not necessarily unique. May be null if the ids of the period
     * are not required.
     */
    @Nullable public Object id;

    /**
     * A unique identifier for the period. May be null if the ids of the period are not required.
     */
    @Nullable public Object uid;

    /**
     * The index of the window to which this period belongs.
     */
    public int windowIndex;

    /**
     * The duration of this period in microseconds, or {@link C#TIME_UNSET} if unknown.
     */
    public long durationUs;

    private long positionInWindowUs;
    private AdPlaybackState adPlaybackState;

    /** Creates a new instance with no ad playback state. */
    public Period() {
      adPlaybackState = AdPlaybackState.NONE;
    }

    /**
     * Sets the data held by this period.
     *
     * @param id An identifier for the period. Not necessarily unique. May be null if the ids of the
     *     period are not required.
     * @param uid A unique identifier for the period. May be null if the ids of the period are not
     *     required.
     * @param windowIndex The index of the window to which this period belongs.
     * @param durationUs The duration of this period in microseconds, or {@link C#TIME_UNSET} if
     *     unknown.
     * @param positionInWindowUs The position of the start of this period relative to the start of
     *     the window to which it belongs, in milliseconds. May be negative if the start of the
     *     period is not within the window.
     * @return This period, for convenience.
     */
    public Period set(
        @Nullable Object id,
        @Nullable Object uid,
        int windowIndex,
        long durationUs,
        long positionInWindowUs) {
      return set(id, uid, windowIndex, durationUs, positionInWindowUs, AdPlaybackState.NONE);
    }

    /**
     * Sets the data held by this period.
     *
     * @param id An identifier for the period. Not necessarily unique. May be null if the ids of the
     *     period are not required.
     * @param uid A unique identifier for the period. May be null if the ids of the period are not
     *     required.
     * @param windowIndex The index of the window to which this period belongs.
     * @param durationUs The duration of this period in microseconds, or {@link C#TIME_UNSET} if
     *     unknown.
     * @param positionInWindowUs The position of the start of this period relative to the start of
     *     the window to which it belongs, in milliseconds. May be negative if the start of the
     *     period is not within the window.
     * @param adPlaybackState The state of the period's ads, or {@link AdPlaybackState#NONE} if
     *     there are no ads.
     * @return This period, for convenience.
     */
    public Period set(
        @Nullable Object id,
        @Nullable Object uid,
        int windowIndex,
        long durationUs,
        long positionInWindowUs,
        AdPlaybackState adPlaybackState) {
      this.id = id;
      this.uid = uid;
      this.windowIndex = windowIndex;
      this.durationUs = durationUs;
      this.positionInWindowUs = positionInWindowUs;
      this.adPlaybackState = adPlaybackState;
      return this;
    }

    /**
     * Returns the duration of the period in milliseconds, or {@link C#TIME_UNSET} if unknown.
     */
    public long getDurationMs() {
      return C.usToMs(durationUs);
    }

    /**
     * Returns the duration of this period in microseconds, or {@link C#TIME_UNSET} if unknown.
     */
    public long getDurationUs() {
      return durationUs;
    }

    /**
     * Returns the position of the start of this period relative to the start of the window to which
     * it belongs, in milliseconds. May be negative if the start of the period is not within the
     * window.
     */
    public long getPositionInWindowMs() {
      return C.usToMs(positionInWindowUs);
    }

    /**
     * Returns the position of the start of this period relative to the start of the window to which
     * it belongs, in microseconds. May be negative if the start of the period is not within the
     * window.
     */
    public long getPositionInWindowUs() {
      return positionInWindowUs;
    }

    /** Returns the opaque identifier for ads played with this period, or {@code null} if unset. */
    @Nullable
    public Object getAdsId() {
      return adPlaybackState.adsId;
    }

    /** Returns the number of ad groups in the period. */
    public int getAdGroupCount() {
      return adPlaybackState.adGroupCount;
    }

    /**
     * Returns the time of the ad group at index {@code adGroupIndex} in the period, in
     * microseconds.
     *
     * @param adGroupIndex The ad group index.
     * @return The time of the ad group at the index relative to the start of the enclosing {@link
     *     Period}, in microseconds, or {@link C#TIME_END_OF_SOURCE} for a post-roll ad group.
     */
    public long getAdGroupTimeUs(int adGroupIndex) {
      return adPlaybackState.adGroupTimesUs[adGroupIndex];
    }

    /**
     * Returns the index of the first ad in the specified ad group that should be played, or the
     * number of ads in the ad group if no ads should be played.
     *
     * @param adGroupIndex The ad group index.
     * @return The index of the first ad that should be played, or the number of ads in the ad group
     *     if no ads should be played.
     */
    public int getFirstAdIndexToPlay(int adGroupIndex) {
      return adPlaybackState.adGroups[adGroupIndex].getFirstAdIndexToPlay();
    }

    /**
     * Returns the index of the next ad in the specified ad group that should be played after
     * playing {@code adIndexInAdGroup}, or the number of ads in the ad group if no later ads should
     * be played.
     *
     * @param adGroupIndex The ad group index.
     * @param lastPlayedAdIndex The last played ad index in the ad group.
     * @return The index of the next ad that should be played, or the number of ads in the ad group
     *     if the ad group does not have any ads remaining to play.
     */
    public int getNextAdIndexToPlay(int adGroupIndex, int lastPlayedAdIndex) {
      return adPlaybackState.adGroups[adGroupIndex].getNextAdIndexToPlay(lastPlayedAdIndex);
    }

    /**
     * Returns whether the ad group at index {@code adGroupIndex} has been played.
     *
     * @param adGroupIndex The ad group index.
     * @return Whether the ad group at index {@code adGroupIndex} has been played.
     */
    public boolean hasPlayedAdGroup(int adGroupIndex) {
      return !adPlaybackState.adGroups[adGroupIndex].hasUnplayedAds();
    }

    /**
     * Returns the index of the ad group at or before {@code positionUs} in the period, if that ad
     * group is unplayed. Returns {@link C#INDEX_UNSET} if the ad group at or before {@code
     * positionUs} has no ads remaining to be played, or if there is no such ad group.
     *
     * @param positionUs The period position at or before which to find an ad group, in
     *     microseconds.
     * @return The index of the ad group, or {@link C#INDEX_UNSET}.
     */
    public int getAdGroupIndexForPositionUs(long positionUs) {
      return adPlaybackState.getAdGroupIndexForPositionUs(positionUs, durationUs);
    }

    /**
     * Returns the index of the next ad group after {@code positionUs} in the period that has ads
     * remaining to be played. Returns {@link C#INDEX_UNSET} if there is no such ad group.
     *
     * @param positionUs The period position after which to find an ad group, in microseconds.
     * @return The index of the ad group, or {@link C#INDEX_UNSET}.
     */
    public int getAdGroupIndexAfterPositionUs(long positionUs) {
      return adPlaybackState.getAdGroupIndexAfterPositionUs(positionUs, durationUs);
    }

    /**
     * Returns the number of ads in the ad group at index {@code adGroupIndex}, or
     * {@link C#LENGTH_UNSET} if not yet known.
     *
     * @param adGroupIndex The ad group index.
     * @return The number of ads in the ad group, or {@link C#LENGTH_UNSET} if not yet known.
     */
    public int getAdCountInAdGroup(int adGroupIndex) {
      return adPlaybackState.adGroups[adGroupIndex].count;
    }

    /**
     * Returns the duration of the ad at index {@code adIndexInAdGroup} in the ad group at
     * {@code adGroupIndex}, in microseconds, or {@link C#TIME_UNSET} if not yet known.
     *
     * @param adGroupIndex The ad group index.
     * @param adIndexInAdGroup The ad index in the ad group.
     * @return The duration of the ad, or {@link C#TIME_UNSET} if not yet known.
     */
    public long getAdDurationUs(int adGroupIndex, int adIndexInAdGroup) {
      AdPlaybackState.AdGroup adGroup = adPlaybackState.adGroups[adGroupIndex];
      return adGroup.count != C.LENGTH_UNSET ? adGroup.durationsUs[adIndexInAdGroup] : C.TIME_UNSET;
    }

    /**
     * Returns the position offset in the first unplayed ad at which to begin playback, in
     * microseconds.
     */
    public long getAdResumePositionUs() {
      return adPlaybackState.adResumePositionUs;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !getClass().equals(obj.getClass())) {
        return false;
      }
      Period that = (Period) obj;
      return Util.areEqual(id, that.id)
          && Util.areEqual(uid, that.uid)
          && windowIndex == that.windowIndex
          && durationUs == that.durationUs
          && positionInWindowUs == that.positionInWindowUs
          && Util.areEqual(adPlaybackState, that.adPlaybackState);
    }

    @Override
    public int hashCode() {
      int result = 7;
      result = 31 * result + (id == null ? 0 : id.hashCode());
      result = 31 * result + (uid == null ? 0 : uid.hashCode());
      result = 31 * result + windowIndex;
      result = 31 * result + (int) (durationUs ^ (durationUs >>> 32));
      result = 31 * result + (int) (positionInWindowUs ^ (positionInWindowUs >>> 32));
      result = 31 * result + adPlaybackState.hashCode();
      return result;
    }
  }

  /** An empty timeline. */
  public static final Timeline EMPTY =
      new Timeline() {

        @Override
        public int getWindowCount() {
          return 0;
        }

        @Override
        public Window getWindow(int windowIndex, Window window, long defaultPositionProjectionUs) {
          throw new IndexOutOfBoundsException();
        }

        @Override
        public int getPeriodCount() {
          return 0;
        }

        @Override
        public Period getPeriod(int periodIndex, Period period, boolean setIds) {
          throw new IndexOutOfBoundsException();
        }

        @Override
        public int getIndexOfPeriod(Object uid) {
          return C.INDEX_UNSET;
        }

        @Override
        public Object getUidOfPeriod(int periodIndex) {
          throw new IndexOutOfBoundsException();
        }
      };

  /**
   * Returns whether the timeline is empty.
   */
  public final boolean isEmpty() {
    return getWindowCount() == 0;
  }

  /**
   * Returns the number of windows in the timeline.
   */
  public abstract int getWindowCount();

  /**
   * Returns the index of the window after the window at index {@code windowIndex} depending on the
   * {@code repeatMode} and whether shuffling is enabled.
   *
   * @param windowIndex Index of a window in the timeline.
   * @param repeatMode A repeat mode.
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return The index of the next window, or {@link C#INDEX_UNSET} if this is the last window.
   */
  public int getNextWindowIndex(
      int windowIndex, @Player.RepeatMode int repeatMode, boolean shuffleModeEnabled) {
    switch (repeatMode) {
      case Player.REPEAT_MODE_OFF:
        return windowIndex == getLastWindowIndex(shuffleModeEnabled) ? C.INDEX_UNSET
            : windowIndex + 1;
      case Player.REPEAT_MODE_ONE:
        return windowIndex;
      case Player.REPEAT_MODE_ALL:
        return windowIndex == getLastWindowIndex(shuffleModeEnabled)
            ? getFirstWindowIndex(shuffleModeEnabled) : windowIndex + 1;
      default:
        throw new IllegalStateException();
    }
  }

  /**
   * Returns the index of the window before the window at index {@code windowIndex} depending on the
   * {@code repeatMode} and whether shuffling is enabled.
   *
   * @param windowIndex Index of a window in the timeline.
   * @param repeatMode A repeat mode.
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return The index of the previous window, or {@link C#INDEX_UNSET} if this is the first window.
   */
  public int getPreviousWindowIndex(
      int windowIndex, @Player.RepeatMode int repeatMode, boolean shuffleModeEnabled) {
    switch (repeatMode) {
      case Player.REPEAT_MODE_OFF:
        return windowIndex == getFirstWindowIndex(shuffleModeEnabled) ? C.INDEX_UNSET
            : windowIndex - 1;
      case Player.REPEAT_MODE_ONE:
        return windowIndex;
      case Player.REPEAT_MODE_ALL:
        return windowIndex == getFirstWindowIndex(shuffleModeEnabled)
            ? getLastWindowIndex(shuffleModeEnabled) : windowIndex - 1;
      default:
        throw new IllegalStateException();
    }
  }

  /**
   * Returns the index of the last window in the playback order depending on whether shuffling is
   * enabled.
   *
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return The index of the last window in the playback order, or {@link C#INDEX_UNSET} if the
   *     timeline is empty.
   */
  public int getLastWindowIndex(boolean shuffleModeEnabled) {
    return isEmpty() ? C.INDEX_UNSET : getWindowCount() - 1;
  }

  /**
   * Returns the index of the first window in the playback order depending on whether shuffling is
   * enabled.
   *
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return The index of the first window in the playback order, or {@link C#INDEX_UNSET} if the
   *     timeline is empty.
   */
  public int getFirstWindowIndex(boolean shuffleModeEnabled) {
    return isEmpty() ? C.INDEX_UNSET : 0;
  }

  /**
   * Populates a {@link Window} with data for the window at the specified index.
   *
   * @param windowIndex The index of the window.
   * @param window The {@link Window} to populate. Must not be null.
   * @return The populated {@link Window}, for convenience.
   */
  public final Window getWindow(int windowIndex, Window window) {
    return getWindow(windowIndex, window, /* defaultPositionProjectionUs= */ 0);
  }

  /** @deprecated Use {@link #getWindow(int, Window)} instead. Tags will always be set. */
  @Deprecated
  public final Window getWindow(int windowIndex, Window window, boolean setTag) {
    return getWindow(windowIndex, window, /* defaultPositionProjectionUs= */ 0);
  }

  /**
   * Populates a {@link Window} with data for the window at the specified index.
   *
   * @param windowIndex The index of the window.
   * @param window The {@link Window} to populate. Must not be null.
   * @param defaultPositionProjectionUs A duration into the future that the populated window's
   *     default start position should be projected.
   * @return The populated {@link Window}, for convenience.
   */
  public abstract Window getWindow(
      int windowIndex, Window window, long defaultPositionProjectionUs);

  /**
   * Returns the number of periods in the timeline.
   */
  public abstract int getPeriodCount();

  /**
   * Returns the index of the period after the period at index {@code periodIndex} depending on the
   * {@code repeatMode} and whether shuffling is enabled.
   *
   * @param periodIndex Index of a period in the timeline.
   * @param period A {@link Period} to be used internally. Must not be null.
   * @param window A {@link Window} to be used internally. Must not be null.
   * @param repeatMode A repeat mode.
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return The index of the next period, or {@link C#INDEX_UNSET} if this is the last period.
   */
  public final int getNextPeriodIndex(
      int periodIndex,
      Period period,
      Window window,
      @Player.RepeatMode int repeatMode,
      boolean shuffleModeEnabled) {
    int windowIndex = getPeriod(periodIndex, period).windowIndex;
    if (getWindow(windowIndex, window).lastPeriodIndex == periodIndex) {
      int nextWindowIndex = getNextWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
      if (nextWindowIndex == C.INDEX_UNSET) {
        return C.INDEX_UNSET;
      }
      return getWindow(nextWindowIndex, window).firstPeriodIndex;
    }
    return periodIndex + 1;
  }

  /**
   * Returns whether the given period is the last period of the timeline depending on the {@code
   * repeatMode} and whether shuffling is enabled.
   *
   * @param periodIndex A period index.
   * @param period A {@link Period} to be used internally. Must not be null.
   * @param window A {@link Window} to be used internally. Must not be null.
   * @param repeatMode A repeat mode.
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return Whether the period of the given index is the last period of the timeline.
   */
  public final boolean isLastPeriod(
      int periodIndex,
      Period period,
      Window window,
      @Player.RepeatMode int repeatMode,
      boolean shuffleModeEnabled) {
    return getNextPeriodIndex(periodIndex, period, window, repeatMode, shuffleModeEnabled)
        == C.INDEX_UNSET;
  }

  /**
   * Calls {@link #getPeriodPosition(Window, Period, int, long, long)} with a zero default position
   * projection.
   */
  public final Pair<Object, Long> getPeriodPosition(
      Window window, Period period, int windowIndex, long windowPositionUs) {
    return Assertions.checkNotNull(
        getPeriodPosition(
            window, period, windowIndex, windowPositionUs, /* defaultPositionProjectionUs= */ 0));
  }

  /**
   * Converts (windowIndex, windowPositionUs) to the corresponding (periodUid, periodPositionUs).
   *
   * @param window A {@link Window} that may be overwritten.
   * @param period A {@link Period} that may be overwritten.
   * @param windowIndex The window index.
   * @param windowPositionUs The window time, or {@link C#TIME_UNSET} to use the window's default
   *     start position.
   * @param defaultPositionProjectionUs If {@code windowPositionUs} is {@link C#TIME_UNSET}, the
   *     duration into the future by which the window's position should be projected.
   * @return The corresponding (periodUid, periodPositionUs), or null if {@code #windowPositionUs}
   *     is {@link C#TIME_UNSET}, {@code defaultPositionProjectionUs} is non-zero, and the window's
   *     position could not be projected by {@code defaultPositionProjectionUs}.
   */
  @Nullable
  public final Pair<Object, Long> getPeriodPosition(
      Window window,
      Period period,
      int windowIndex,
      long windowPositionUs,
      long defaultPositionProjectionUs) {
    Assertions.checkIndex(windowIndex, 0, getWindowCount());
    getWindow(windowIndex, window, defaultPositionProjectionUs);
    if (windowPositionUs == C.TIME_UNSET) {
      windowPositionUs = window.getDefaultPositionUs();
      if (windowPositionUs == C.TIME_UNSET) {
        return null;
      }
    }
    int periodIndex = window.firstPeriodIndex;
    long periodPositionUs = window.getPositionInFirstPeriodUs() + windowPositionUs;
    long periodDurationUs = getPeriod(periodIndex, period, /* setIds= */ true).getDurationUs();
    while (periodDurationUs != C.TIME_UNSET && periodPositionUs >= periodDurationUs
        && periodIndex < window.lastPeriodIndex) {
      periodPositionUs -= periodDurationUs;
      periodDurationUs = getPeriod(++periodIndex, period, /* setIds= */ true).getDurationUs();
    }
    return Pair.create(Assertions.checkNotNull(period.uid), periodPositionUs);
  }

  /**
   * Populates a {@link Period} with data for the period with the specified unique identifier.
   *
   * @param periodUid The unique identifier of the period.
   * @param period The {@link Period} to populate. Must not be null.
   * @return The populated {@link Period}, for convenience.
   */
  public Period getPeriodByUid(Object periodUid, Period period) {
    return getPeriod(getIndexOfPeriod(periodUid), period, /* setIds= */ true);
  }

  /**
   * Populates a {@link Period} with data for the period at the specified index. {@link Period#id}
   * and {@link Period#uid} will be set to null.
   *
   * @param periodIndex The index of the period.
   * @param period The {@link Period} to populate. Must not be null.
   * @return The populated {@link Period}, for convenience.
   */
  public final Period getPeriod(int periodIndex, Period period) {
    return getPeriod(periodIndex, period, false);
  }

  /**
   * Populates a {@link Period} with data for the period at the specified index.
   *
   * @param periodIndex The index of the period.
   * @param period The {@link Period} to populate. Must not be null.
   * @param setIds Whether {@link Period#id} and {@link Period#uid} should be populated. If false,
   *     the fields will be set to null. The caller should pass false for efficiency reasons unless
   *     the fields are required.
   * @return The populated {@link Period}, for convenience.
   */
  public abstract Period getPeriod(int periodIndex, Period period, boolean setIds);

  /**
   * Returns the index of the period identified by its unique {@link Period#uid}, or {@link
   * C#INDEX_UNSET} if the period is not in the timeline.
   *
   * @param uid A unique identifier for a period.
   * @return The index of the period, or {@link C#INDEX_UNSET} if the period was not found.
   */
  public abstract int getIndexOfPeriod(Object uid);

  /**
   * Returns the unique id of the period identified by its index in the timeline.
   *
   * @param periodIndex The index of the period.
   * @return The unique id of the period.
   */
  public abstract Object getUidOfPeriod(int periodIndex);

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Timeline)) {
      return false;
    }
    Timeline other = (Timeline) obj;
    if (other.getWindowCount() != getWindowCount() || other.getPeriodCount() != getPeriodCount()) {
      return false;
    }
    Timeline.Window window = new Timeline.Window();
    Timeline.Period period = new Timeline.Period();
    Timeline.Window otherWindow = new Timeline.Window();
    Timeline.Period otherPeriod = new Timeline.Period();
    for (int i = 0; i < getWindowCount(); i++) {
      if (!getWindow(i, window).equals(other.getWindow(i, otherWindow))) {
        return false;
      }
    }
    for (int i = 0; i < getPeriodCount(); i++) {
      if (!getPeriod(i, period, /* setIds= */ true)
          .equals(other.getPeriod(i, otherPeriod, /* setIds= */ true))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    Window window = new Window();
    Period period = new Period();
    int result = 7;
    result = 31 * result + getWindowCount();
    for (int i = 0; i < getWindowCount(); i++) {
      result = 31 * result + getWindow(i, window).hashCode();
    }
    result = 31 * result + getPeriodCount();
    for (int i = 0; i < getPeriodCount(); i++) {
      result = 31 * result + getPeriod(i, period, /* setIds= */ true).hashCode();
    }
    return result;
  }
}
