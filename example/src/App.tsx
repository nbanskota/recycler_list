import { StyleSheet, View } from 'react-native';

import RecyclerListView from '../../src';
import { groups, livetvData } from './liveTv_channel_items';

interface ListItem {
  title: string;
  index: number;
  items: any;
}

export default function App() {
  const formattedData = groups
    .map((groupItem) => {
      const items = livetvData.filter((item) =>
        groupItem.keys.includes(item.key)
      );
      return {
        title: groupItem.name,
        index: groupItem.index,
        items: items,
      } as ListItem;
    })
    .sort((x, y) => x.index - y.index);

  const handleItemPress = (event: any) => {
    console.log({ index: event.index });
  };

  const handleFocusChange = (event: any) => {
    console.log({ focus: event.index });
  };

  return (
    <View style={styles.container}>
      <RecyclerListView
        data={formattedData}
        columns={3}
        onItemPress={handleItemPress}
        onFocusChange={handleFocusChange}
        style={styles.recyclerView}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'flex-end',
    transform: [{ scale: 0.8 }],
  },
  recyclerView: {
    flex: 1,
    width: '100%',
    overflow: 'visible',
  },
});
