import {
  StyleSheet,
  TouchableOpacity,
  View,
  Text,
  findNodeHandle,
} from 'react-native';

import RecyclerListView, { ExitDirection, FocusableViews } from '../../src';
import { groups, livetvData } from './liveTv_channel_items';
import { useEffect, useRef, useState } from 'react';

interface ListItem<T> {
  title: string;
  index: number;
  items: [T];
}

export default function App() {
  const recyclerViewRef = useRef(null);
  const ref1 = useRef<View>(null);
  const ref2 = useRef<View>(null);
  const ref3 = useRef<View>(null);
  const ref4 = useRef<View>(null);

  const [focusableViews, setFocusableViews] = useState<FocusableViews>();

  const [selectedState, setSelectedState] = useState('green');
  useEffect(() => {
    setFocusableViews({
      top: findNodeHandle(ref1.current),
      bottom: findNodeHandle(ref4.current),
      left: findNodeHandle(ref2.current),
      right: findNodeHandle(ref3.current),
    });
  }, []);

  const formattedData = groups
    .map((groupItem) => {
      const items = livetvData.filter((item) =>
        groupItem.keys.includes(item.key)
      );
      return {
        title: groupItem.name,
        index: groupItem.index,
        items: items,
      } as ListItem<any>;
    })
    .sort((x, y) => x.index - y.index);

  const handleItemPress = (event: any) => {
    console.log({ pressedItem: event.index });
  };

  const handleFocusChange = (event: any) => {
    console.log({ focus: event.index });
  };
  const handleFocusExit = (direction: ExitDirection) => {
    console.log({
      exitDirection: direction,
    });
    switch (direction) {
      case 'left':
        ref1.current?.focus();

        break;
      case 'right':
        ref2.current?.focus();
        break;
      case 'up':
        ref3.current?.focus();
        break;
      case 'down':
        ref4.current?.focus();
        break;
    }
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity
        focusable
        onFocus={() => setSelectedState('blue')}
        onBlur={() => setSelectedState('green')}
        style={{
          position: 'absolute',
          top: 0,
          alignSelf: 'center',
          zIndex: 1,
        }}
      >
        <View focusable ref={ref1} style={{ backgroundColor: selectedState }}>
          <Text>Top</Text>
        </View>
      </TouchableOpacity>

      <TouchableOpacity
        focusable
        onFocus={() => setSelectedState('blue')}
        onBlur={() => setSelectedState('green')}
        style={{
          position: 'absolute',
          left: 0,
          alignSelf: 'center',
          zIndex: 1,
        }}
      >
        <View focusable ref={ref2} style={{ backgroundColor: selectedState }}>
          <Text>Left</Text>
        </View>
      </TouchableOpacity>
      <TouchableOpacity
        focusable
        onFocus={() => setSelectedState('blue')}
        onBlur={() => setSelectedState('green')}
        style={{
          position: 'absolute',
          right: 0,
          alignSelf: 'center',
          zIndex: 1,
        }}
      >
        <View focusable ref={ref3} style={{ backgroundColor: selectedState }}>
          <Text>Right</Text>
        </View>
      </TouchableOpacity>
      <TouchableOpacity
        focusable
        onFocus={() => setSelectedState('blue')}
        onBlur={() => setSelectedState('green')}
        style={{
          position: 'absolute',
          bottom: 0,
          alignSelf: 'center',
          zIndex: 1,
        }}
      >
        <View focusable ref={ref4} style={{ backgroundColor: selectedState }}>
          <Text>Bottom</Text>
        </View>
      </TouchableOpacity>

      <RecyclerListView
        ref={recyclerViewRef}
        data={formattedData}
        config={{
          columnCount: 3,
          direction: 1,
          itemSpan: [2, 1, 1],
        }}
        onItemPress={handleItemPress}
        onItemFocusChange={handleFocusChange}
        exitDirection={handleFocusExit}
        style={styles.recyclerView}
        focusableViews={focusableViews}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',

    transform: [{ scale: 1 }],
    padding: 0,
    backgroundColor: 'black',
  },
  recyclerView: {
    flex: 1,
    width: '100%',
    overflow: 'hidden',
  },
});
