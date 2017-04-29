import React from "react";
import Gauge from "./charts/Gauge";
import {inject, observer} from "mobx-react";
import ThreadPoolPane from "./ThreadPoolPane";
import ResourcesPane from "./ResourcesPane";


@inject("metricsStore") @observer
export default class Home extends React.Component {

    componentWillMount() {
        this.props.metricsStore.fetch();
    }

    render() {

        const {metrics} = this.props.metricsStore;

        return (
            <div class="main-content">
                <div class="container">
                    <div class="row">

                        <div class="area-top clearfix">
                            <div class="pull-left header">
                                <h3 class="title">
                                    <i class="icon-dashboard"></i>
                                    Dashboard
                                </h3>
                                <h5>
                                    <span>
                                      A subtitle can go here
                                    </span>
                                </h5>
                            </div>

                            <ul class="list-inline pull-right sparkline-box">

                                <li class="sparkline-row">
                                    <h4 class="blue"><span>Orders</span> 847</h4>
                                    {/*<div class="sparkline big" data-color="blue"><!--23,3,6,25,5,5,19,18,4,12,5,13--></div>*/}
                                </li>

                                <li class="sparkline-row">
                                    <h4 class="green"><span>Reviews</span> 223</h4>
                                    {/*<div class="sparkline big" data-color="green"><!--21,21,7,20,19,5,17,10,12,16,9,12--></div>*/}
                                </li>

                                <li class="sparkline-row">
                                    <h4 class="red"><span>New visits</span> 7930</h4>
                                    {/*<div class="sparkline big"><!--19,26,25,11,14,13,19,18,13,14,16,22--></div>*/}
                                </li>

                            </ul>
                        </div>
                    </div>
                </div>

                <div class="container">

                    <div class="row">
                        <div class="col-md-12">
                            <div class="box">
                                <div class="box-header"><span class="title">Memory</span></div>
                                <div class="box-content padded" style={{textAlign: 'center'}}>
                                    <Gauge key={'heapUsage'}
                                           name={'heapUsage'}
                                           value={metrics.usedMemory}
                                           min={0}
                                           max={metrics.totalMemory}
                                           title={'Heap Usage'}
                                           label={'MB'}/>

                                    <Gauge key={'heapSize'}
                                           name={'heapSize'}
                                           value={metrics.totalMemory}
                                           min={0}
                                           max={metrics.maxMemory}
                                           title={'Heap Size'}
                                           label={'MB'}/>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <ResourcesPane resources={metrics.resources}/>
                        </div>
                        <div class="col-md-6">
                            <ThreadPoolPane threadPools={metrics.threadPools}/>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}