/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.networking.crafting;


import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;


public interface ICraftingJob
{

	/**
	 * @return if this job is a simulation, simulations cannot be submitted and only represent 1 possible future
	 * crafting job with fake items.
	 */
	boolean isSimulation();

	/**
	 * @return total number of bytes to process this job.
	 */
	long getByteTotal();

	/**
	 * Populates the plan list with stack size, and requestable values that represent the stored, and crafting job
	 * contents respectively.
	 *
	 * @param plan plan
	 */
	void populatePlan( IItemList<IAEItemStack> plan );

	/**
	 * @return the final output of the job.
	 */
	IAEItemStack getOutput();

	/**
	 * Retrieves the total number of crafting rounds required for the given item as the primary output.
	 * The primary output is defined as the first output item in a crafting pattern.
	 *
	 * @param keyItem The item to check, which must match the primary output of relevant patterns.
	 * Use {@link IAEItemStack#reset()} to ignore stack size when matching.
	 * @return Total crafting count for the item as primary output. Returns 0 if the item
	 * is not a primary output in any relevant pattern or simulation isn't complete.
	 * @throws IllegalArgumentException If keyItem is null or invalid.
	 * @throws IllegalStateException If called before simulation completes (when {@link #isSimulation()} returns false).
	 */
	long getTotalCraftsForPrimaryOutput(IAEItemStack keyItem);
}
