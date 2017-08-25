from __future__ import absolute_import
from __future__ import division
from __future__ import nested_scopes
from __future__ import print_function
import logging

def print_log(worker_num, arg):
  print("{0}: {1}".format(worker_num, arg))

def map_fun(args, ctx):
  from tensorflowonspark import TFNode
  from datetime import datetime
  import math
  import numpy
  import tensorflow as tf
  import time

  worker_num = ctx.worker_num
  job_name = ctx.job_name
  task_index = ctx.task_index
  cluster_spec = ctx.cluster_spec

  if job_name == "ps":
    time.sleep((worker_num + 1) * 5)

  batch_size = args.batch_size

  cluster, server = TFNode.start_cluster_server(ctx, 1)

  def feed_dict(batch):
    images = []
    labels = []
    for item in batch:
      images.append(item[0])
      labels.append(item[1])
    x_initial = numpy.array(images)
    x_objdump = x_initial[:,519:719]
    x_cnn = numpy.empty((0, 200), dtype=numpy.float64)
    for i in xrange(len(images)):  
      x_cnn_batch = numpy.zeros((200, 120), dtype=numpy.float64)
      for j in xrange(0, 200):
        x_cnn_batch[j, int(x_objdump[i, j])] = True
      x_cnn_batch = numpy.transpose(x_cnn_batch)
      x_cnn = numpy.append(x_cnn, x_cnn_batch, axis=0)
    x_peinfo = x_initial[:,0:519]
    ys = numpy.array(labels)
    return (x_peinfo.reshape(-1,519,1,1),x_cnn.reshape(-1, 200, 120, 1), ys)

  def conv2d(x, W):
      return tf.nn.conv2d(x, W, strides=[1, 1, 1, 1], padding='SAME')

  def max_pool_1(x):
      return tf.nn.avg_pool(x, ksize=[1, 2,1, 1], strides=[1, 2, 1, 1], padding='SAME')

  def max_pool_2(x):
      return tf.nn.avg_pool(x, ksize=[1, 100,1, 1], strides=[1, 100, 1, 1], padding='SAME')

  if job_name == "ps":
    server.join()
  elif job_name == "worker":
    with tf.device(tf.train.replica_device_setter(
        worker_device="/job:worker/task:%d" % task_index,
        cluster=cluster)):
      # Build NN-Network
      W_mlp_1 = tf.Variable(tf.truncated_normal([519,519],stddev=0.1), name="W_mlp_1") 
      b_mlp_1 = tf.Variable(tf.constant(0.1, shape=[519]),name="b_mlp_1")
      tf.summary.histogram("W_mlp_1", W_mlp_1)
      W_mlp_2 = tf.Variable(tf.truncated_normal([519,519],stddev=0.1), name="W_mlp_2") 
      b_mlp_2 = tf.Variable(tf.constant(0.1, shape=[519]),name="b_mlp_2") 
      tf.summary.histogram("W_mlp_2", W_mlp_2)   

      W_conv1 = tf.Variable(tf.truncated_normal([3,120,1,3],stddev=0.1), name="W_conv1") 
      b_conv1 = tf.Variable(tf.constant(0.1, shape=[3]),name="b_conv1")
      tf.summary.histogram("W_conv1", W_conv1)
      W_conv2 = tf.Variable(tf.truncated_normal([3,120,3,6],stddev=0.1),name="W_conv2") 
      b_conv2 = tf.Variable(tf.constant(0.1, shape=[6]),name="b_conv2")
      tf.summary.histogram("W_conv2", W_conv2)

      sm_w = tf.Variable(tf.truncated_normal([1239, 10], stddev= 0.1), name="sm_w")
      sm_b = tf.Variable(tf.constant(0.1, shape=[10]),name="sm_b")
      tf.summary.histogram("softmax_weights", sm_w)

      x_cnn = tf.placeholder(tf.float32, [None, 200,120,1], name="x_cnn")
      x_mlp = tf.placeholder(tf.float32, [None, 519,1,1], name="x_mlp")
      y_ = tf.placeholder(tf.float32, [None, 10], name="y_")
      tf.summary.image("x_cnn", x_cnn)
      tf.summary.image("x_mlp", x_mlp)

      x_mlp_new = tf.reshape(x_mlp, [-1, 519])
      h_mlp_1 = tf.nn.xw_plus_b(x_mlp_new, W_mlp_1, b_mlp_1)
      h_mlp_2 = tf.nn.xw_plus_b(h_mlp_1, W_mlp_2, b_mlp_2)
      h_conv1 = tf.nn.relu(conv2d(x_cnn, W_conv1) + b_conv1)
      h_pool1 = max_pool_1(h_conv1)
      h_conv2 = tf.nn.relu(conv2d(h_pool1, W_conv2) + b_conv2)
      h_pool2 = max_pool_2(h_conv2)
      h_conv2_flat = tf.reshape(h_pool2, [-1, 120*6])

      h_inter = tf.concat([h_mlp_2, h_conv2_flat],1)
      y = tf.nn.softmax(tf.nn.xw_plus_b(h_inter, sm_w, sm_b))

      global_step = tf.Variable(0)
      loss = tf.reduce_sum(tf.nn.softmax_cross_entropy_with_logits(logits=y, labels=y_))
      tf.summary.scalar("loss", loss)
      train_op = tf.train.AdagradOptimizer(0.001).minimize(
          loss, global_step=global_step)

      label = tf.argmax(y_, 1, name="label")
      prediction = tf.argmax(y, 1,name="prediction")
      correct_prediction = tf.equal(prediction, label)
      accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32), name="accuracy")
      tf.summary.scalar("acc", accuracy)

      saver = tf.train.Saver()
      summary_op = tf.summary.merge_all()
      init_op = tf.global_variables_initializer()

    logdir = TFNode.hdfs_path(ctx, args.model)
    print("tensorflow model path: {0}".format(logdir))
    summary_writer = tf.summary.FileWriter("tensorboard_%d" %(worker_num), graph=tf.get_default_graph())

    if args.mode == "train":
      sv = tf.train.Supervisor(is_chief=(task_index == 0),
                               logdir=logdir,
                               init_op=init_op,
                               summary_op=None,
                               saver=saver,
                               global_step=global_step,
                               stop_grace_secs=300,
                               save_model_secs=10)
    else:
      sv = tf.train.Supervisor(is_chief=(task_index == 0),
                               logdir=logdir,
                               summary_op=None,
                               saver=saver,
                               global_step=global_step,
                               stop_grace_secs=300,
                               save_model_secs=0)

    with sv.managed_session(server.target) as sess:
      print("{0} session ready".format(datetime.now().isoformat()))
      step = 0
      tf_feed = TFNode.DataFeed(ctx.mgr, args.mode == "train")
      while not sv.should_stop() and not tf_feed.should_stop() and step < args.steps:
        batch_mlp, batch_xs, batch_ys = feed_dict(tf_feed.next_batch(batch_size))
        feed = {x_mlp: batch_mlp, x_cnn: batch_xs, y_: batch_ys}

        if len(batch_xs) > 0:
          if args.mode == "train":
            _, summary, step = sess.run([train_op, summary_op, global_step], feed_dict=feed)
            if (step % 10 == 0):
              print("{0} step: {1} accuracy: {2}".format(datetime.now().isoformat(), step, sess.run(accuracy,{x_mlp: batch_mlp, x_cnn: batch_xs, y_: batch_ys})))
            if sv.is_chief:
              summary_writer.add_summary(summary, step)
          
          elif args.mode == "inference": 
            labels, preds, acc = sess.run([label, prediction, accuracy], feed_dict=feed)
            results = ["Label: {0}, Prediction: {1}".format(l, p) for l,p in zip(labels,preds)]
            tf_feed.batch_results(results)
            print("acc: {0}".format(acc))

          else:
            preds= sess.run(prediction, feed_dict={x_mlp: batch_mlp, x_cnn: batch_xs})
            results = ["Sha256: {0}, Prediction: {1}".format(l, p) for l,p in zip(batch_ys,preds)]
            tf_feed.batch_results(results)
            print(results)
            
      if sv.should_stop() or step >= args.steps:
        tf_feed.terminate()

    print("{0} stopping supervisor".format(datetime.now().isoformat()))
    sv.stop()

