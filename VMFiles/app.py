
from werkzeug.utils import secure_filename
from flask import Flask, jsonify, render_template, request, redirect, url_for
import os
import numpy as np
import scipy
import librosa
import json

import pickle
import time
start_time = time.time()

ar2 ={}
ar2['C'] = ar2['B#'] = 1
ar2['C#'] = ar2['Db'] = 2
ar2['D'] = 3
ar2['D#'] = ar2['Eb'] = 4
ar2['E'] = ar2['Fb'] = 5
ar2['F'] = ar2['E#'] = 6
ar2['F#'] = ar2['Gb'] = 7
ar2['G'] = 8
ar2['G#'] = ar2['Ab'] = 9
ar2['A']  = 10
ar2['A#'] = ar2['Bb'] = 11
ar2['B'] = ar2['Cb'] = 12


ar3 = {}
ar3[1] = 'C'
ar3[2] = 'C#'
ar3[3] = 'D'
ar3[4] = 'D#'
ar3[5] = 'E'
ar3[6] = 'F'
ar3[7] = 'F#'
ar3[8] = 'G'
ar3[9] = 'G#'
ar3[10] = 'A'
ar3[11] = 'A#'
ar3[12] = 'B'
#importing bigrams dictionary
with open("Bigrams_Dict_Type.txt", "rb") as myFile:
    BigramsDictionary = pickle.load(myFile)
with open("Trigrams_Dict_Type.txt", "rb") as myFile2:
    TrigramsDictionary = pickle.load(myFile2)

UPLOAD_FOLDER = '/home/christina/uploads/'

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

@app.route('/upload', methods = ['GET', 'POST'])
def dynamic_page():
  if request.method == 'POST':
    submitted_file = request.files['file']
    if submitted_file:
      filename = secure_filename(submitted_file.filename)
      submitted_file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
      audio_file_path = UPLOAD_FOLDER + filename
  else:
    audio_file_path = '/home/christina/Neutral Milk Hotel - In The Aeroplane Over The Sea.wav'

  data_folder = '/home/christina/data'
  # DL model and DL model's code
  # Created by Vivien Seguy on 2020/12/27.
  # Copyright © 2020 Vivien Seguy. All rights reserved.
  
  
  ## Load audio data
  sample_freq = 22050 # target audio frequency
  audio_data, _ = librosa.load(audio_file_path, sr=sample_freq, mono=True)


  ## Stack sliding windows vertically
  frame_length = 2048
  hop_length = 512
  frame_starts_sample_indices = range(0, audio_data.size - frame_length + 1, hop_length)  # indices of the first sample of each  sliding window (window = frame)
  audio_frame_array = np.vstack([audio_data[start:start+frame_length] for start in frame_starts_sample_indices])


  ## Compute the mel spectrogram of each window
  n_fft = frame_length
  mel_power = 2
  n_mels = 128
  hamming_window = np.hamming(frame_length).astype(np.float32)

  # 1) Take the fft of each frame, compute its magnitude and power to get the spectrograms
  audio_frame_array_times_window = audio_frame_array * np.reshape(hamming_window, newshape=(1, frame_length))
  fft_array = scipy.fft.fft(audio_frame_array_times_window, axis=1)
  fft_array_truncated = fft_array[:, 0:int(frame_length / 2 + 1)]
  fft_array_truncated_power = np.abs(fft_array_truncated) ** mel_power

  # 2) Load the mel filters, and multiply them with the previous obtained spectrogrames
  mel_filter_bank = np.load(os.path.join(data_folder, 'mel_filter_bank.npy'))
  mel_spectrogram_array = np.matmul(fft_array_truncated_power, np.transpose(mel_filter_bank))

  # 3) Take the log
  log_mel_spectrogram_array = np.log(1. + mel_spectrogram_array)


  ## Gather frame features into groups and normalize them. A group will be an input to the model.
  ## A group of frames can be seen as an image (with channel size=1, height=group_frame_number, width=n_mels)
  frame_number = log_mel_spectrogram_array.shape[0]
  group_frame_number = 16 # number of frames per group
  group_hop_frame_number = 16 # two consecutive groups are group_hop_frame_number frames away

  def compute_local_norm(features, epsilon=1e-6): # function for normalizing each group values
       return (features - np.mean(features)) / np.maximum(np.std(features), epsilon)

  features_groups = [compute_local_norm(log_mel_spectrogram_array[start:start + group_frame_number, :]) \
                          for start in range(0, frame_number - group_frame_number + 1, group_hop_frame_number)]
  stacked_feature_groups = np.stack(features_groups)

  # the model input dimensions will correspond to (batch_size, sequence_size, channels, height, width)
  model_input = np.transpose(stacked_feature_groups, (0, 2, 1)) # the log mel spectrogram bins are the width, the number of frames in a group is the height
  model_input = np.expand_dims(model_input, 0) # batch size = 1
  model_input = np.expand_dims(model_input, 2) # channel size = 1
  model_input = model_input.astype(np.float32) # final dims should be (1, ?, 1, 16, 128) ('?' is the variable sequence size)

  # Create the hidden states which are used as inputs for the LSTM submodel
  h_in = np.zeros((2,1,512)).astype(np.float32) # (num_layers, batch_size, hidden_size)
  c_in = np.zeros((2,1,512)).astype(np.float32) # (num_layers, batch_size, hidden_size)


  ## Load the onnx model
  model_file_path = os.path.join(data_folder, 'model.onnx')
  os.remove(audio_file_path)

  import onnxruntime
  sess = onnxruntime.InferenceSession(model_file_path)
  (chord_logits, bass_logits, h_out, c_out) = sess.run(None, {'input': model_input, 'h_in': h_in, 'c_in': c_in})
  chord_indices = np.argmax(chord_logits, axis=1)
  bass_indices = np.argmax(bass_logits, axis=1)

  with open(os.path.join(data_folder, 'F0_and_color_labels.json')) as file:
      F0_and_color_labels = json.load(file)

  with open(os.path.join(data_folder, 'notes.json')) as file:
      notes = json.load(file)

  print('Inferred chords:')
  chords = [F0_and_color_labels[i] for i in chord_indices]
  print(chords)

  print('Inferred bass:')
  basses = [notes[i] for i in bass_indices]
  print(basses)
  

  ## Let's print the chords and basses on a file together with the timings
  group_length = frame_length + (group_frame_number - 1) * hop_length  # number of audio samples in a group
  group_duration = group_length / sample_freq # time span of a group
  sequence_group_number = stacked_feature_groups.shape[0] # number of groups in the sequence
  sample_number_between_groups = group_hop_frame_number * hop_length
  group_hop_duration = sample_number_between_groups / sample_freq
  groups_time_line = np.array([group_index * group_hop_duration + group_duration / 2. for group_index in range(sequence_group_number)])

  with open('inference_result.txt', 'w') as file:
    for group_time, chord, bass in zip(groups_time_line, chords, basses):
        chord_tonic = chord[:2] if (len(chord) > 1 and chord[1] in ['b', '#']) else chord[:1]
        if chord_tonic != bass:
            chord = chord + '/' + bass
        file.write('{:6.4}  {}\n'.format(group_time, chord))
  ##END of DL model's code
  
  
  #TODO: write time in chords
  previous = ''
  final = []
  final_timer = []
  final_bass = []
  for i in range (len(chords)):
    chord = chords[i]
    if(chord != previous):
      final.append(chord)
      final_bass.append(basses[i])
      final_timer.append("{:.2f}".format(groups_time_line[i]))
    previous=chord
  #starting intervals - temporary list
  intervals = []
  #chord intervals
  chord_intervals=[]
  #chord type (maj/min)
  chord_type = []
  #chord intervals should be starting from 0
  chord_simple = final[0].split('m')[0]
  k = ar2[chord_simple] #gia na xekiname apo to 0 tha afairesw k apo ola.

  #turning chords into numbers
  for chord in final:
    chord_simple = chord.split('m')[0]
    chord_t = 'min' if 'm' in chord else 'maj'
    intervals.append((ar2[chord_simple]-k+1)%12)
    chord_type.append(chord_t)
  #filling the chord_intervals list by checking with previous element in list every time
  chord_intervals.append(0)
  # print(intervals)
  temp=intervals[0]
  for i in intervals[1:]:
    chord_intervals.append((i-temp)%12)
    temp=i
    # print(chord_intervals)
  alternative_chords = []
  alternative_chords2 = []
  for position in range(len(final)):
    max = max2 = z_max = z_max2 = -1
    z_type = z_type2 = ''
    x = chord_intervals[position]
    x_type = chord_type[position]
    x_prev = chord_intervals[position - 1] if position > 0 else ''
    x_next = chord_intervals[position + 1] if position < len(final)-1 else ''
    x_prev_type = chord_type[position - 1] if position > 0 else ''
    x_next_type = chord_type[position + 1] if position < len(final)-1 else ''

    #testing with changed chord as min:
    for z in range(0,12):
    # if posiiton = 0 , we can only check with the bigram [z ztype, x_next x_next_type] and trigram [z ztype, x1 x1_type, x2 x2_type]
      if (position == 0):
        # create bigram key in the form of [z min, x_next x_next_type]
        key_next_min = "[{0} {2}, {1} {3}]".format(z, x_next, 'min', x_next_type)
        # create bigram key in the form of [z maj, x_next x_next_type]
        key_next_maj = "[{0} {2}, {1} {3}]".format(z, x_next, 'maj', x_next_type)
        # create trigram key in the form of [z zmin, x1 x1_type, x2 x2_type]
        key_tri_min = "[{0} {1}, {2} {3}, {4} {5}]".format(z, 'min', x_next, x_next_type, chord_intervals[2], chord_type[2]) if (len(final)>2) else ""
        # create trigram key in the form of [z zmaj, x1 x1_type, x2 x2_type]
        key_tri_maj = "[{0} {1}, {2} {3}, {4} {5}]".format(z, 'maj', x_next, x_next_type, chord_intervals[2], chord_type[2]) if (len(final)>2) else ""
        # if the key exists in dictionary, add its value to the "following" variable, else key has zero occurencies in the train dataset, so following = 0 
        following_min = BigramsDictionary[key_next_min] if key_next_min in BigramsDictionary else 0
        following_maj = BigramsDictionary[key_next_maj] if key_next_maj in BigramsDictionary else 0
        # check in the trigrams dictionary if key exists, else key has zero occurencies in the train dataset, so tri = 0 
        tri_min = TrigramsDictionary[key_tri_min] if key_tri_min in TrigramsDictionary else 0
        tri_maj = TrigramsDictionary[key_tri_maj] if key_tri_maj in TrigramsDictionary else 0
        # find sum using tri+bi results
        sum_min = following_min + tri_min
        sum_maj = following_maj + tri_maj 
      # if position = len(numerals)-1, we can only check with bigram [x_prev x_prev_type, z z_type] and trigram
      elif (position == len(final)-1):
      # create bigram key in the form of [x_prev x_prev_type, z z_type]
        key_prev_min = "[{0} {2}, {1} {3}]".format(x_prev, z, x_prev_type, 'min')
        key_prev_maj = "[{0} {2}, {1} {3}]".format(x_prev, z, x_prev_type, 'maj')
        # create trigram key in the form of [z z_type, x1 x1_type, x2 x2_type]
        key_tri_min = "[{0} {1}, {2} {3}, {4} {5}]".format(chord_intervals[position - 2], chord_type[position - 2],x_prev, x_prev_type, z, 'min') if (len(final)>2) else ""
        key_tri_maj = "[{0} {1}, {2} {3}, {4} {5}]".format(chord_intervals[position - 2], chord_type[position - 2],x_prev, x_prev_type, z, 'maj') if (len(final)>2) else ""
        # if the key exists in dictionary, add its value to the "previous" variable else key has zero occurencies in the train dataset, so previous = 0 
        previous_min = BigramsDictionary[key_next_min] if key_next_min in BigramsDictionary else 0
        previous_maj = BigramsDictionary[key_next_maj] if key_next_maj in BigramsDictionary else 0
        # check in the trigrams dictionary if key exists, else key has zero occurencies in the train dataset, so tri = 0 
        tri_min = TrigramsDictionary[key_tri_min] if key_tri_min in TrigramsDictionary else 0
        tri_maj = TrigramsDictionary[key_tri_maj] if key_tri_maj in TrigramsDictionary else 0
        # find sum using tri+bi results
        sum_min = previous_min + tri_min
        sum_maj = previous_maj + tri_maj
      else:
      # create bigram key in the form of [x_prev x_prev_type, z type]
        key_prev_min = "[{0} {2}, {1} {3}]".format(x_prev, z, x_prev_type, 'min')
        key_prev_maj = "[{0} {2}, {1} {3}]".format(x_prev, z, x_prev_type, 'maj')
        # create bigram key in the form of [z type, x_next x_next_type]
        key_next_min = "[{0} {2}, {1} {3}]".format(z, x_next, 'min', x_next_type)
        key_next_maj = "[{0} {2}, {1} {3}]".format(z, x_next, 'maj', x_next_type)
        # create trigram key in the form of [z ztype, x1 x1_type, x2 x2_type]
        key_tri_min = "[{0} {1}, {2} {3}, {4} {5}]".format(x_prev,  x_prev_type, z, 'min', x_next, x_next_type) if (len(final)>2) else ""
        key_tri_maj = "[{0} {1}, {2} {3}, {4} {5}]".format(x_prev,  x_prev_type, z, 'maj', x_next, x_next_type) if (len(final)>2) else ""
        # if the key exists in dictionary, add its value to the "previous" variable else key has zero occurencies in the train dataset, so previous = 0 
        previous_min = BigramsDictionary[key_next_min] if key_next_min in BigramsDictionary else 0
        previous_maj = BigramsDictionary[key_next_maj] if key_next_maj in BigramsDictionary else 0
        # check in the trigrams dictionary if key exists, else key has zero occurencies in the train dataset, so tri = 0 
        tri_min = TrigramsDictionary[key_tri_min] if key_tri_min in TrigramsDictionary else 0
        tri_maj = TrigramsDictionary[key_tri_maj] if key_tri_maj in TrigramsDictionary else 0
        # find sum using tri+bi results with the right weights
        sum_min = tri_min + previous_min*0.70 + following_min*0.30
        sum_maj = tri_maj + previous_maj*0.70 + following_maj*0.30
        # if sum > max, set max = sum and keep the value of z and z_type in z_max and z_type
      if(sum_min > max and z!=chord_intervals[position]):
        max2 = max
        z_max2 = z_max
        z_type2 = z_type
        max = sum_min
        z_max = z
        z_type = 'min'
      elif(sum_min>max2 and z!=chord_intervals[position]):
        max=sum_min
        z_max2 = z
        z_type2 = 'min'
      if(sum_maj > max and z!=chord_intervals[position]):
        max2 = max
        z_max2 = z_max
        z_type2 = z_type
        max = sum_min
        z_max = z
        z_type = 'maj'
      elif(sum_min>max2 and z!=chord_intervals[position]):
        max=sum_min
        z_max2 = z
        z_type2 = 'maj'

    chord_intervals_temp = []
    chord_intervals_temp2 = []
    for chord_interval in chord_intervals:
        chord_intervals_temp.append(chord_interval)
        chord_intervals_temp2.append(chord_interval)
    chord_intervals_temp[position] = z_max
    chord_intervals_temp2[position] = z_max2

    new_intervals = []
    new_intervals2 = []
  
    #an position>0
    if(position!=0):
        new_intervals.append(intervals[0])
        new_intervals2.append(intervals[0])
    else:
        new_intervals.append(z_max)
        new_intervals2.append(z_max2)

    prev=0
    for i in range(1, len(chord_intervals_temp)):
        new_intervals.append((chord_intervals_temp[i] + intervals[prev])%12)
        new_intervals2.append((chord_intervals_temp2[i] + intervals[prev])%12)
        prev += 1


    addition = 'm' if (z_type=='min') else ''
    addition2 = 'm' if (z_type2=='min') else ''

    variable = (new_intervals[position]+k-1)%12
    if (variable == 0 ): variable = 12
    alternative_chords.append((ar3[variable])+addition)

    # for 2nd suggestion:
    variable = (new_intervals2[position]+k-1)%12
    if (variable == 0 ): variable = 12
    alternative_chords2.append((ar3[variable])+addition2)
  print(final)
  print(alternative_chords)
  print("--- %s seconds ---" % (time.time() - start_time))

  return json.dumps({"Chords": (final), "Bass": (final_bass), "Time": (final_timer), "AlternativeChords": (alternative_chords), "AlternativeChords2": (alternative_chords2)})

