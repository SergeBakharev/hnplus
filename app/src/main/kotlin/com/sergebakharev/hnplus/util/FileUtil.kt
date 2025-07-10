package com.sergebakharev.hnplus.util

import android.os.AsyncTask
import android.util.Log
import com.sergebakharev.hnplus.App
import com.sergebakharev.hnplus.model.HNCommentTreeNode
import com.sergebakharev.hnplus.model.HNFeed
import com.sergebakharev.hnplus.model.HNPostComments
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object FileUtil {
    private const val LAST_HNFEED_FILENAME = "lastHNFeed"
    private const val LAST_HNPOSTCOMMENTS_FILENAME_PREFIX = "lastHNPostComments"
    private const val TAG = "FileUtil"

    private var lastHNFeed: HNFeed?
        /*
             * Returns null if no last feed was found or could not be parsed.
             */
        get() {
            var obj: ObjectInputStream? = null
            try {
                obj =
                    ObjectInputStream(FileInputStream(lastHNFeedFilePath))
                val rawHNFeed = obj.readObject()
                if (rawHNFeed is HNFeed) return rawHNFeed
            } catch (e: Exception) {
                Log.e(TAG, "Could not get last HNFeed from file :(", e)
            } finally {
                if (obj != null) {
                    try {
                        obj.close()
                    } catch (e: IOException) {
                        Log.e(TAG, "Couldn't close last NH feed file :(", e)
                    }
                }
            }
            return null
        }
        set(hnFeed) {
            Run.inBackground {
                var os: ObjectOutputStream? = null
                try {
                    os =
                        ObjectOutputStream(FileOutputStream(lastHNFeedFilePath))
                    os.writeObject(hnFeed)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not save last HNFeed to file :(", e)
                } finally {
                    if (os != null) {
                        try {
                            os.close()
                        } catch (e: IOException) {
                            Log.e(
                                TAG,
                                "Couldn't close last NH feed file :(",
                                e
                            )
                        }
                    }
                }
            }
        }

    private val lastHNFeedFilePath: String
        get() {
            val dataDir: File = App.getInstance()!!.filesDir
            return dataDir.absolutePath + File.pathSeparator + LAST_HNFEED_FILENAME
        }

    /*
    * Returns null if no last comments file was found or could not be parsed.
    */
    private fun getLastHNPostComments(postID: String): HNPostComments? {
        var obj: ObjectInputStream? = null
        try {
            obj = ObjectInputStream(FileInputStream(getLastHNPostCommentsPath(postID)))
            val rawHNComments = obj.readObject()
            if (rawHNComments is HNPostComments) return rawHNComments
        } catch (e: Exception) {
            Log.e(TAG, "Could not get last HNPostComments from file :(", e)
        } finally {
            if (obj != null) {
                try {
                    obj.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Couldn't close last NH comments file :(", e)
                }
            }
        }
        return null
    }

    fun setLastHNPostComments(comments: HNPostComments, postID: String) {
        Run.inBackground(Runnable {
            var os: ObjectOutputStream? = null
            try {
                val nodesCount = countNodes(comments.treeNodes)
                if (nodesCount > 150) {
                    return@Runnable
                }
                os = ObjectOutputStream(
                    FileOutputStream(
                        getLastHNPostCommentsPath(postID)
                    )
                )
                os.writeObject(comments)
            } catch (e: Exception) {
                Log.e(TAG, "Could not save last HNPostComments to file :(", e)
            } finally {
                if (os != null) {
                    try {
                        os.close()
                    } catch (e: IOException) {
                        Log.e(
                            TAG,
                            "Couldn't close last NH comments file :(",
                            e
                        )
                    }
                }
            }
        })
    }

    private fun getLastHNPostCommentsPath(postID: String): String {
        val dataDir: File = App.getInstance()!!.filesDir
        return dataDir.absolutePath + "/" + LAST_HNPOSTCOMMENTS_FILENAME_PREFIX + "_" + postID
    }

    private fun countNodes(nodes: List<HNCommentTreeNode>?): Int {
        var sum = 0

        if (nodes != null) {
            sum += nodes.size

            for (n in nodes) {
                sum += countNodes(n.children)
            }
        }

        return sum
    }

    fun saveLastHNFeed(hnFeed: HNFeed?) {
        lastHNFeed = hnFeed
    }

    abstract class GetLastHNFeedTask : AsyncTask<Void?, Void?, HNFeed?>() {
        override fun doInBackground(vararg params: Void?): HNFeed? {
            return lastHNFeed
        }
    }

    abstract class GetLastHNPostCommentsTask :
        AsyncTask<String?, Void?, HNPostComments?>() {
        override fun doInBackground(vararg postIDs: String?): HNPostComments? {
            if (postIDs != null && postIDs.size > 0) return getLastHNPostComments(postIDs[0] ?: "")
            return null
        }
    }
}
