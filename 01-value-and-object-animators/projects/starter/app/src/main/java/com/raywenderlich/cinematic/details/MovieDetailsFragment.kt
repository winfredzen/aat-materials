/*
 * Copyright (c) 2021 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.raywenderlich.cinematic.details

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.BlurTransformation
import com.raywenderlich.cinematic.R
import com.raywenderlich.cinematic.databinding.FragmentDetailsBinding
import com.raywenderlich.cinematic.model.Movie
import com.raywenderlich.cinematic.util.Constants.IMAGE_BASE
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class MovieDetailsFragment : Fragment(R.layout.fragment_details) {

  private var _binding: FragmentDetailsBinding? = null
  private val binding get() = _binding!!

  private val args: MovieDetailsFragmentArgs by navArgs()
  private val viewModel: MovieDetailsViewModel by viewModel()

  private val castAdapter: CastAdapter by inject()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentDetailsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.castList.apply {
      adapter = castAdapter
      layoutManager =
        LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    args.movieId.also {
      viewModel.getMovieDetails(it)
      viewModel.getCast(it)
    }
    attachObservers()
  }

  private fun attachObservers() {
    viewModel.movie.observe(viewLifecycleOwner) { movie ->
      renderUi(movie)
    }

    viewModel.cast.observe(viewLifecycleOwner) { cast ->
      castAdapter.submitList(cast)
    }
  }

  /**
   * 刷新UI
   */
  private fun renderUi(movie: Movie) {
    loadBackdrop(IMAGE_BASE + movie.backdropPath)
    loadPoster(IMAGE_BASE + movie.posterPath)

    binding.title.text = movie.title
    binding.summary.text = movie.overview
    binding.ratingValue.text = movie.rating.toString()
    binding.movieRating.rating = movie.rating

    if (viewModel.shouldAnimate) {
      //TODO animate the summary

      // 动画
      animateText(binding.title)
      animateText(binding.summary)
      animateText(binding.ratingValue)
      animateText(binding.movieRating)
    }

    binding.addToFavorites.apply {
      icon = if (movie.isFavorite) {
        getDrawable(requireContext(), R.drawable.ic_baseline_favorite_24)
      } else {
        getDrawable(requireContext(), R.drawable.ic_baseline_favorite_border_24)
      }
      text = if (movie.isFavorite) {
        getString(R.string.remove_from_favorites)
      } else {
        getString(R.string.add_to_favorites)
      }
      setOnClickListener {
        if (movie.isFavorite) {
          viewModel.unsetMovieAsFavorite(movie.id)
        } else {
          viewModel.setMovieAsFavorite(movie.id)
        }
      }
    }
  }

  private fun loadPoster(posterUrl: String) {
    val posterRequest = ImageRequest.Builder(requireContext())
      .data(posterUrl)
      .target {
        binding.posterContainer.isVisible = true
        binding.poster.setImageDrawable(it)
        if (viewModel.shouldAnimate) {
          //TODO animate poster
          // 动画
          animatePoster()
        }
      }.build()
    requireContext().imageLoader.enqueue(posterRequest)
  }

  private fun loadBackdrop(backdropUrl: String) {
    val posterRequest = ImageRequest.Builder(requireContext())
      .data(backdropUrl)
      .transformations(BlurTransformation(requireContext()))
      .target {
        binding.backdrop.isVisible = true
        binding.backdrop.setImageDrawable(it)
        if (viewModel.shouldAnimate) {
          //TODO animate backdrop

          // 动画
          animateBackdrop()
        }
      }.build()
    requireContext().imageLoader.enqueue(posterRequest)
  }

  // 海报动画
  private fun animatePoster() {
    binding.posterContainer.alpha = 0f
    val animator = ValueAnimator.ofFloat(0f, 1f)
    animator.duration = 1000
    // 插值
    // 输入为均匀变化0~1.0f之间浮点值，输出为先加速超过临界值1.0f 再慢慢又回落到1.0f 连续变化的浮点值
    animator.interpolator = OvershootInterpolator()
    animator.addUpdateListener {
      val animatedValue = it.animatedValue as Float
      binding.posterContainer.alpha = animatedValue
      binding.posterContainer.scaleX = animatedValue
      binding.posterContainer.scaleY = animatedValue
    }
    animator.start()
  }


  // 背景动画 从底部弹出
  private fun animateBackdrop() {
    val finalYPosition = binding.backdrop.y

    val startYPosition = finalYPosition + 40
    binding.backdrop.y = startYPosition

    val animator = ValueAnimator.ofFloat(startYPosition, finalYPosition)
    animator.duration = 1000
    animator.interpolator = DecelerateInterpolator()

    animator.addUpdateListener { valueAnimator ->
      val animatedValue = valueAnimator.animatedValue as Float
      binding.backdrop.translationY = animatedValue
    }
    animator.start()
  }

  private fun animateText(view: View) {
    val objectAnimator = ObjectAnimator.ofFloat(
      view,
      "alpha",
      0f,
      1f
    )

    objectAnimator.duration = 1500
    objectAnimator.start()
  }


  //
//  private fun animateText() {
//    val animator = ObjectAnimator.ofFloat(binding.summary, "alpha", 0f, 1f)
//    animator.duration = 1000
//    animator.start()
//  }



}